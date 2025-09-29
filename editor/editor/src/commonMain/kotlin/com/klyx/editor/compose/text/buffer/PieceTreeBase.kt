@file:Suppress("ConstPropertyName")

package com.klyx.editor.compose.text.buffer

import com.klyx.editor.compose.text.CharCode
import com.klyx.editor.compose.text.LineFeedCounter
import com.klyx.editor.compose.text.Position
import com.klyx.editor.compose.text.Range
import com.klyx.editor.compose.text.Strings
import com.klyx.editor.compose.text.codePointAt
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.jvm.JvmName
import kotlin.math.floor

@Serializable
internal class PieceTreeBase {
    @Transient
    private val lock = reentrantLock()

    var lineCount = 1
        private set
    var length = 0
        private set

    @set:JvmName("setEndOfLine")
    var lineBreak: String
        private set

    private var lineBreakLength: Int
    private var lineBreakNormalized: Boolean

    private var lastChangeBufferPosition = BufferCursor()
    private var lastVisitedLine = VisitedLine(0, "")

    private var searchCache = PieceTreeSearchCache(1)
    private val buffers = mutableListOf(TextBuffer())

    // the root node of red-black tree
    @Serializable(with = TreeNodeSerializer::class)
    var root: TreeNode = Sentinel

    companion object {
        // the string chunk size 64k
        private const val AverageBufferSize: Int = 65535
    }

    constructor(
        chunks: List<TextBuffer>,
        lineBreak: String,
        lineBreakNormalized: Boolean = false
    ) {
        this.lineBreak = lineBreak
        this.lineBreakLength = lineBreak.length
        this.lineBreakNormalized = lineBreakNormalized
        create(chunks, lineBreak, lineBreakNormalized)
    }

    private fun create(chunks: List<TextBuffer>, lineBreak: String, lineBreakNormalized: Boolean) {
        this.lineBreak = lineBreak
        this.lineBreakLength = lineBreak.length
        this.lineBreakNormalized = lineBreakNormalized

        var lastNode = NullTreeNode
        for ((index, chunk) in chunks.withIndex()) {
            if (chunk.buffer.isNotEmpty()) {
                if (chunk.lineStarts.isEmpty()) {
                    chunk.lineStarts = chunk.buffer.computeLineStartOffsets()
                }

                val piece = Piece(
                    bufferIndex = index + 1,
                    start = BufferCursor.Zero,
                    end = BufferCursor(
                        line = chunk.lineStarts.lastIndex,
                        column = chunk.buffer.length - chunk.lineStarts.last()
                    ),
                    lineFeedCnt = chunk.lineStarts.lastIndex,
                    length = chunk.buffer.length
                )

                buffers.add(chunk)
                lastNode = rbInsertRight(lastNode, piece)
            }
        }

        computeBufferMetadata()
    }

    private fun normalizeLineBreak(newLineBreak: String) {
        lock.withLock {
            val min = AverageBufferSize - floor(AverageBufferSize / 3f).toInt()
            val max = min * 2

            val tempChunk = StringBuilder()
            var tempChunkLen = 0
            val chunks = mutableListOf<TextBuffer>()

            iterateTree {
                val str = it.content()
                val len = str.length
                if (tempChunkLen <= min || tempChunkLen + len < max) {
                    tempChunk.append(str)
                    tempChunkLen += len
                    return@iterateTree true
                }

                // flush anyways
                val text = tempChunk.replace(Strings.newLine, newLineBreak)
                chunks.add(TextBuffer(StringBuilder(text), text.computeLineStartOffsets()))
                tempChunk.setRange(0, tempChunk.length, str)
                tempChunkLen = len

                true
            }

            if (tempChunkLen > 0) {
                val text = tempChunk.toString().replace(Strings.newLine, newLineBreak)
                chunks.add(TextBuffer(StringBuilder(text), text.computeLineStartOffsets()))
            }

            create(chunks, lineBreak, true)
        }
    }

    fun setLineBreak(lineBreak: String) {
        this.lineBreak = lineBreak
        this.lineBreakLength = lineBreak.length
        normalizeLineBreak(newLineBreak = lineBreak)
    }

    fun createSnapshot(BOM: String) = PieceTreeSnapshot(this, BOM)

    override fun equals(other: Any?): Boolean {
        if (other !is PieceTreeBase) return false

        if (length != other.length) return false
        if (lineCount != other.lineCount) return false

        var offset = 0
        return iterateTree {
            if (it === Sentinel) return@iterateTree true

            val str = it.content()
            val len = str.length
            val startPosition = other.nodeAt(offset)
            val endPosition = other.nodeAt(offset + len)
            val value = other.getValueInRange(startPosition, endPosition)

            @Suppress("AssignedValueIsNeverRead")
            offset += len

            str == value
        }
    }

    fun offsetAt(lineNumber: Int, column: Int): Int = lock.withLock {
        var leftLen = 0 // inorder
        var line = lineNumber

        var x = root
        while (x !== Sentinel) {
            if (x.left !== Sentinel && x.lfLeft + 1 >= line) {
                x = x.left
            } else if (x.lfLeft + x.piece.lineFeedCnt + 1 >= line) {
                leftLen += x.sizeLeft
                // lineNumber >= 2
                val accumulatedValInCurrentIndex = x.getAccumulatedValue(line - x.lfLeft - 2)
                leftLen += accumulatedValInCurrentIndex + column - 1
                return leftLen
            } else {
                line -= x.lfLeft + x.piece.lineFeedCnt
                leftLen += x.sizeLeft + x.piece.length
                x = x.right
            }
        }

        return leftLen
    }

    fun positionAt(index: Int): Position = lock.withLock {
        var offset = maxOf(0, floor(index.toDouble()).toInt())

        var x = root
        var lfCnt = 0
        val originalOffset = offset

        while (x !== Sentinel) {
            if (x.sizeLeft != 0 && x.sizeLeft >= offset) {
                x = x.left
            } else if (x.sizeLeft + x.piece.length >= offset) {
                val (index, remainder) = x.indexOf(offset - x.sizeLeft)

                lfCnt += x.lfLeft + index

                if (index == 0) {
                    val lineStartOffset = offsetAt(lfCnt + 1, 1)
                    val column = originalOffset - lineStartOffset
                    return Position(lfCnt + 1, column + 1)
                }

                return Position(lfCnt + 1, remainder + 1)
            } else {
                offset -= x.sizeLeft + x.piece.length
                lfCnt += x.lfLeft + x.piece.lineFeedCnt

                if (x.right === Sentinel) {
                    // last node
                    val lineStartOffset = offsetAt(lfCnt + 1, 1)
                    val column = originalOffset - offset - lineStartOffset
                    return Position(lfCnt + 1, column + 1)
                } else {
                    x = x.right
                }
            }
        }

        return Position(1, 1)
    }

    private fun TreeNode.positionInBuffer(remainder: Int, ret: BufferCursor? = null) = lock.withLock {
        val bufferIndex = piece.bufferIndex
        val lineStarts = buffers[bufferIndex].lineStarts

        val startOffset = lineStarts[piece.start.line] + piece.start.column

        val offset = startOffset + remainder

        // binary search offset between startOffset and endOffset
        var low = piece.start.line
        var high = piece.end.line

        var mid = 0
        var midStop: Int
        var midStart = 0

        while (low <= high) {
            // mid = low + ((high - low) / 2) or 0
            mid = (low + high) shr 1
            midStart = lineStarts[mid]

            if (mid == high) {
                break
            }

            midStop = lineStarts[mid + 1]

            if (offset < midStart) {
                high = mid - 1
            } else if (offset >= midStop) {
                low = mid + 1
            } else {
                break
            }
        }

        if (ret != null) {
            ret.line = mid
            ret.column = offset - midStart
            return@withLock ret
        }

        BufferCursor(mid, offset - midStart)
    }

    // return (index, remainder)
    private fun TreeNode.indexOf(accumulatedValue: Int) = lock.withLock {
        val pos = positionInBuffer(accumulatedValue)
        val lineCnt = pos.line - piece.start.line

        if (
            offsetInBuffer(piece.bufferIndex, piece.end) -
            offsetInBuffer(piece.bufferIndex, piece.start) == accumulatedValue
        ) {
            // we are checking the end of this node, so a CRLF check is necessary.
            val realLineCnt = getLineFeedCnt(piece.bufferIndex, piece.start, pos)
            if (realLineCnt != lineCnt) {
                // aha yes, CRLF
                return@withLock Pair(realLineCnt, 0)
            }
        }

        Pair(lineCnt, pos.column)
    }

    private fun getLineFeedCnt(bufferIndex: Int, start: BufferCursor, end: BufferCursor): Int {
        // we don't need to worry about start: abc\r|\n, or abc|\r, or abc|\n, or abc|\r\n doesn't
        // change the fact that, there is one line break after start.
        // now let's take care of end: abc\r|\n, if end is in between \r and \n, we need to add line
        // feed count by 1
        if (end.column == 0) return end.line - start.line

        val lineStarts = buffers[bufferIndex].lineStarts
        if (end.line == lineStarts.size - 1) { // it means, there is no \n after end, otherwise, there will be one more lineStart.
            return end.line - start.line
        }

        val nextLineStartOffset = lineStarts[end.line + 1]
        val endOffset = lineStarts[end.line] + end.column
        if (nextLineStartOffset > endOffset + 1) { // there are more than 1 character after end, which means it can't be \n
            return end.line - start.line
        }
        // endOffset + 1 === nextLineStartOffset
        // character at endOffset is \n, so we check the character before first
        // if character at endOffset is \r, end.column is 0 and we can't get here.
        val previousCharOffset = endOffset - 1 // end.column > 0 so it's okay.
        val buffer = buffers[bufferIndex].buffer

        return if (buffer.codePointAt(previousCharOffset) == 13) {
            end.line - start.line + 1
        } else {
            end.line - start.line
        }
    }

    private fun TreeNode.getAccumulatedValue(index: Int): Int = lock.withLock {
        // check the index
        if (index < 0) return 0

        val lineStarts = buffers[piece.bufferIndex].lineStarts
        val expectedLineStartIndex = piece.start.line + index + 1

        return if (expectedLineStartIndex > piece.end.line) {
            lineStarts[piece.end.line] + piece.end.column - lineStarts[piece.start.line] - piece.start.column
        } else {
            lineStarts[expectedLineStartIndex] - lineStarts[piece.start.line] - piece.start.column
        }
    }

    fun getValueInRange(range: Range, lineBreak: String? = null): String = lock.withLock {
        if (range.isEmpty()) return ""

        val startPosition = nodeAt(range.startLine, range.startColumn)
        val endPosition = nodeAt(range.endLine, range.endColumn)
        val value = getValueInRange(startPosition, endPosition)

        if (lineBreak != null) {
            if (lineBreak !== this.lineBreak || !lineBreakNormalized) {
                return value.replace(Strings.newLine, lineBreak)
            }

            if (lineBreak == this.lineBreak && lineBreakNormalized) {
                if (lineBreak == "\r\n") {
                    // nothing to do
                }
                return value
            }

            return value.replace(Strings.newLine, lineBreak)
        }

        return value
    }

    fun getValueInRange(startPosition: NodePosition, endPosition: NodePosition) = lock.withLock {
        if (startPosition.node === endPosition.node) {
            val node = startPosition.node
            val buffer = buffers[node.piece.bufferIndex].buffer
            val startOffset = offsetInBuffer(node.piece.bufferIndex, node.piece.start)
            return@withLock buffer.substring(
                startOffset + startPosition.remainder,
                startOffset + endPosition.remainder
            )
        }

        var x = startPosition.node
        var buffer = buffers[x.piece.bufferIndex].buffer
        var startOffset = offsetInBuffer(x.piece.bufferIndex, x.piece.start)
        val ret = StringBuilder(
            buffer.substring(
                startOffset + startPosition.remainder,
                startOffset + x.piece.length
            )
        )

        x = x.next()
        while (x != Sentinel) {
            buffer = buffers[x.piece.bufferIndex].buffer
            startOffset = offsetInBuffer(x.piece.bufferIndex, x.piece.start)

            if (x === endPosition.node) {
                ret.append(buffer.substring(startOffset, startOffset + endPosition.remainder))
                break
            } else {
                ret.append(buffer.substring(startOffset, startOffset + x.piece.length))
            }

            x = x.next()
        }

        ret.toString()
    }

    fun lines() = lock.withLock {
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()
        var danglingCR = false

        iterateTree {
            if (it === Sentinel) return@iterateTree true

            val piece = it.piece
            var pieceLength = piece.length
            if (pieceLength == 0) return@iterateTree true

            val buffer = buffers[piece.bufferIndex].buffer
            val lineStarts = buffers[piece.bufferIndex].lineStarts

            val pieceStartLine = piece.start.line
            val pieceEndLine = piece.end.line
            var pieceStartOffset = lineStarts[pieceStartLine] + piece.start.column

            if (danglingCR) {
                if (buffer.codePointAt(pieceStartOffset) == CharCode.LineFeed) {
                    // pretend the \n was in the previous piece..
                    pieceStartOffset++
                    pieceLength--
                }
                lines.add(currentLine.toString())
                currentLine.deleteRange(0, currentLine.length)
                danglingCR = false
                if (pieceLength == 0) return@iterateTree true
            }

            if (pieceStartLine == pieceEndLine) {
                // this piece has no new lines
                if (!lineBreakNormalized && buffer.codePointAt(pieceStartOffset + pieceLength - 1) == CharCode.CarriageReturn) {
                    danglingCR = true
                    currentLine.append(
                        buffer.substring(pieceStartOffset, pieceStartOffset + pieceLength - 1)
                    )
                } else {
                    currentLine.append(
                        buffer.substring(pieceStartOffset, pieceStartOffset + pieceLength)
                    )
                }
                return@iterateTree true
            }

            // add the text before the first line start in this piece
            currentLine.append(
                if (lineBreakNormalized) {
                    buffer.substring(
                        pieceStartOffset,
                        maxOf(pieceStartOffset, lineStarts[pieceStartLine + 1] - lineBreakLength)
                    )
                } else {
                    buffer.substring(pieceStartOffset, lineStarts[pieceStartLine + 1])
                        .replace(Strings.newLine, "")
                }
            )

            lines.add(currentLine.toString())

            for (line in pieceStartLine + 1 until pieceEndLine) {
                currentLine.append(
                    if (lineBreakNormalized) {
                        buffer.substring(lineStarts[line], lineStarts[line + 1] - lineBreakLength)
                    } else {
                        buffer.substring(lineStarts[line], lineStarts[line + 1])
                            .replace(Strings.newLine, "")
                    }
                )
                lines.add(currentLine.toString())
            }

            if (!lineBreakNormalized && buffer.codePointAt(lineStarts[pieceEndLine] + piece.end.column - 1) == CharCode.CarriageReturn) {
                danglingCR = true
                if (piece.end.column == 0) {
                    // The last line ended with a \r, let's undo the push, it will be pushed by next
                    // iteration
                    // linesLength--
                    lines.removeLast()
                } else {
                    currentLine.setRange(
                        0,
                        currentLine.length,
                        buffer.substring(
                            lineStarts[pieceEndLine],
                            lineStarts[pieceEndLine] + piece.end.column - 1
                        )
                    )
                }
            } else {
                currentLine.setRange(
                    0,
                    currentLine.length,
                    buffer.substring(
                        lineStarts[pieceEndLine],
                        lineStarts[pieceEndLine] + piece.end.column
                    )
                )
            }

            true
        }

        lines.toList()
    }

    fun getLineContent(lineNumber: Int): String = lock.withLock {
        if (lastVisitedLine.lineNumber == lineNumber) {
            return lastVisitedLine.value
        }

        lastVisitedLine.lineNumber = lineNumber

        if (lineNumber == lineCount) {
            lastVisitedLine.value = getLineRawContent(lineNumber)
        } else if (lineBreakNormalized) {
            lastVisitedLine.value = getLineRawContent(lineNumber, lineBreakLength)
        } else {
            lastVisitedLine.value = getLineRawContent(lineNumber).replace(Strings.newLine, "")
        }

        return lastVisitedLine.value
    }

    fun getLineContentWithLineBreak(lineNumber: Int) = lock.withLock {
        getLineContent(lineNumber) + lineBreak
    }

    private fun getCharCodeInternal(nodePos: NodePosition): Int {
        if (nodePos.remainder == nodePos.node.piece.length) {
            // the char we want to fetch is at the head of next node.
            val matchingNode = nodePos.node.next()
            if (matchingNode === NullTreeNode) {
                return 0
            }

            val buffer = buffers[matchingNode.piece.bufferIndex]
            val startOffset = offsetInBuffer(matchingNode.piece.bufferIndex, matchingNode.piece.start)
            return buffer.buffer.codePointAt(startOffset)
        } else {
            val buffer = buffers[nodePos.node.piece.bufferIndex]
            val startOffset = offsetInBuffer(nodePos.node.piece.bufferIndex, nodePos.node.piece.start)
            val targetOffset = startOffset + nodePos.remainder
            return buffer.buffer.codePointAt(targetOffset)
        }
    }

    fun getLineCharCode(lineNumber: Int, index: Int): Int {
        val nodePos = nodeAt(lineNumber, index + 1)
        return getCharCodeInternal(nodePos)
    }

    fun getLineLength(lineNumber: Int) = lock.withLock {
        if (lineNumber == lineCount) {
            val startOffset = offsetAt(lineNumber, 1)
            return@withLock length - startOffset
        }

        offsetAt(lineNumber + 1, 1) - offsetAt(lineNumber, 1) - lineBreakLength
    }

    fun getCharCode(offset: Int) = lock.withLock {
        val nodePos = nodeAt(offset)
        getCharCodeInternal(nodePos)
    }

    /**
     * get nearest chunk of text after `offset` in the text buffer.
     * this method is mainly used for treesitter parsing
     */
    fun getNearestChunk(offset: Int): String = lock.withLock {
        val nodePos = this.nodeAt(offset)
        if (nodePos.remainder == nodePos.node.piece.length) {
            // the offset is at the head of next node.
            val matchingNode = nodePos.node.next()
            if (matchingNode === NullTreeNode || matchingNode === Sentinel) {
                return "" // nothing to do
            }

            val buffer = buffers[matchingNode.piece.bufferIndex]
            val startOffset = offsetInBuffer(matchingNode.piece.bufferIndex, matchingNode.piece.start)
            return buffer.buffer.substring(startOffset, startOffset + matchingNode.piece.length)
        } else {
            val buffer = buffers[nodePos.node.piece.bufferIndex]
            val startOffset = offsetInBuffer(nodePos.node.piece.bufferIndex, nodePos.node.piece.start)
            val targetOffset = startOffset + nodePos.remainder
            val targetEnd = startOffset + nodePos.node.piece.length
            return buffer.buffer.substring(targetOffset, targetEnd)
        }
    }

    private fun deleteNodes(nodes: List<TreeNode>) {
        for (node in nodes) rbDelete(node)
    }

    private fun createNewPieces(value: String): List<Piece> = lock.withLock {
        var text = value
        if (text.length > AverageBufferSize) {
            // the content is large, operations like substring, charCode becomes slow
            // so here we split it into smaller chunks, just like what we did for CR/LF
            // normalization
            val newPieces = mutableListOf<Piece>()
            while (text.length > AverageBufferSize) {
                val lastChar = text.codePointAt(AverageBufferSize - 1)
                var splitText: String
                if (
                    lastChar == CharCode.CarriageReturn ||
                    (lastChar in 0xD800..0xDBFF)
                ) {
                    // last character is \r or a high surrogate => keep it back
                    splitText = text.take(AverageBufferSize - 1)
                    text = text.substring(AverageBufferSize - 1)
                } else {
                    splitText = text.take(AverageBufferSize)
                    text = text.substring(AverageBufferSize)
                }

                val lineStarts = splitText.computeLineStartOffsets()
                newPieces.add(
                    Piece(
                        bufferIndex = buffers.size,
                        start = BufferCursor(0, 0),
                        end = BufferCursor(
                            lineStarts.lastIndex,
                            splitText.length - lineStarts[lineStarts.lastIndex]
                        ),
                        lineFeedCnt = lineStarts.lastIndex,
                        length = splitText.length
                    )
                )
                buffers.add(TextBuffer(StringBuilder(splitText), lineStarts))
            }

            val lineStarts = text.computeLineStartOffsets()
            newPieces.add(
                Piece(
                    buffers.size, /* buffer index */
                    BufferCursor(0, 0),
                    BufferCursor(
                        lineStarts.size - 1,
                        text.length - lineStarts[lineStarts.size - 1]
                    ),
                    lineStarts.size - 1,
                    text.length
                )
            )
            buffers.add(TextBuffer(StringBuilder(text), lineStarts))

            return newPieces
        }

        var startOffset = buffers[0].buffer.length
        val lineStarts = text.computeLineStartOffsets()

        var start = lastChangeBufferPosition
        if (
            buffers[0].lineStarts[buffers[0].lineStarts.size - 1] == startOffset &&
            startOffset != 0 &&
            text.startWithLF() &&
            buffers[0].buffer.endWithCR() // todo, we can check lastChangeBufferPosition's column as it's the last one
        ) {
            lastChangeBufferPosition.line = lastChangeBufferPosition.line
            lastChangeBufferPosition.column += 1
            start = lastChangeBufferPosition

            for (i in 0..<lineStarts.size) {
                lineStarts[i] += startOffset + 1
            }

            buffers[0].lineStarts += (lineStarts.slice(1..lineStarts.lastIndex))
            buffers[0].buffer.append("_").append(text)
            startOffset += 1
        } else {
            if (startOffset != 0) {
                for (i in 0..<lineStarts.size) {
                    lineStarts[i] += startOffset
                }
            }
            buffers[0].lineStarts += (lineStarts.slice(1..<lineStarts.size))
            buffers[0].buffer.append(text)
        }

        val endOffset = buffers[0].buffer.length
        val endIndex = buffers[0].lineStarts.lastIndex
        val endColumn = endOffset - buffers[0].lineStarts[endIndex]
        val endPos = BufferCursor(endIndex, endColumn)
        val newPiece = Piece(
            0,
            /** todo@peng */
            start,
            endPos,
            this.getLineFeedCnt(0, start, endPos),
            endOffset - startOffset
        )
        lastChangeBufferPosition = endPos
        return listOf(newPiece)
    }

    fun getLinesRawContent() = lock.withLock {
        getContentOfSubTree(root)
    }

    fun getLineRawContent(line: Int, endOffset: Int = 0): String = lock.withLock {
        var x = this.root
        var lineNumber = line

        val ret = StringBuilder()
        val cache = searchCache.getByLine(lineNumber)

        if (cache != null) {
            x = cache.node
            val prevAccumulatedValue = x.getAccumulatedValue(lineNumber - cache.nodeStartLineNumber - 1)
            val buffer = buffers[x.piece.bufferIndex].buffer
            val startOffset = offsetInBuffer(x.piece.bufferIndex, x.piece.start)
            if (cache.nodeStartLineNumber + x.piece.lineFeedCnt == lineNumber) {
                ret.append(
                    buffer.substring(
                        startOffset + prevAccumulatedValue,
                        startOffset + x.piece.length
                    )
                )
            } else {
                val accumulatedValue = x.getAccumulatedValue(lineNumber - cache.nodeStartLineNumber)
                return buffer.substring(
                    startOffset + prevAccumulatedValue,
                    startOffset + accumulatedValue - endOffset
                )
            }
        } else {
            var nodeStartOffset = 0
            val originalLineNumber = lineNumber
            while (x !== Sentinel) {
                if (x.left !== Sentinel && x.lfLeft >= lineNumber - 1) {
                    x = x.left
                } else if (x.lfLeft + x.piece.lineFeedCnt > lineNumber - 1) {
                    val prevAccumulatedValue = x.getAccumulatedValue(lineNumber - x.lfLeft - 2)
                    val accumulatedValue = x.getAccumulatedValue(lineNumber - x.lfLeft - 1)
                    val buffer = buffers[x.piece.bufferIndex].buffer
                    val startOffset = this.offsetInBuffer(x.piece.bufferIndex, x.piece.start)
                    nodeStartOffset += x.sizeLeft
                    searchCache.insert(
                        CacheEntry(
                            node = x,
                            nodeStartOffset = nodeStartOffset,
                            nodeStartLineNumber = originalLineNumber - (lineNumber - 1 - x.lfLeft)
                        )
                    )

                    return buffer.substring(
                        startOffset + prevAccumulatedValue,
                        startOffset + accumulatedValue - endOffset
                    )
                } else if (x.lfLeft + x.piece.lineFeedCnt == lineNumber - 1) {
                    val prevAccumulatedValue = x.getAccumulatedValue(lineNumber - x.lfLeft - 2)
                    val buffer = buffers[x.piece.bufferIndex].buffer
                    val startOffset = offsetInBuffer(x.piece.bufferIndex, x.piece.start)
                    // check prevAccumulatedValue
                    if (x.piece.length > prevAccumulatedValue) {
                        ret.setRange(
                            0,
                            ret.length,
                            buffer.substring(
                                startOffset + prevAccumulatedValue,
                                startOffset + x.piece.length
                            )
                        )
                    } else {
                        ret.deleteRange(0, ret.length)
                    }
                    break
                } else {
                    lineNumber -= x.lfLeft + x.piece.lineFeedCnt
                    nodeStartOffset += x.sizeLeft + x.piece.length
                    x = x.right
                }
            }
        }

        // search in order, to find the node contains end column
        x = x.next()
        while (x !== Sentinel) {
            val buffer = buffers[x.piece.bufferIndex].buffer

            if (x.piece.lineFeedCnt > 0) {
                val accumulatedValue = x.getAccumulatedValue(0)
                val startOffset = offsetInBuffer(x.piece.bufferIndex, x.piece.start)

                ret.append(buffer.substring(startOffset, startOffset + accumulatedValue - endOffset))
                return ret.toString()
            } else {
                val startOffset = offsetInBuffer(x.piece.bufferIndex, x.piece.start)
                ret.append(buffer.substring(startOffset, startOffset + x.piece.length))
            }

            x = x.next()
        }

        return ret.toString()
    }

    /**
     * Multiline search always executes on the lines concatenated with \n. We must therefore
     * compensate for the count of \n in case the model is CRLF
     */
    @Suppress("unused")
    private fun getMultilineMatchRangeInternal(
        text: String,
        deltaOffset: Int,
        lfCounter: LineFeedCounter?,
        matchIndex: Int,
        value: String
    ) = lock.withLock {
        var startOffset: Int
        var endOffset: Int
        var lineFeedCountBeforeMatch: Int

        if (lfCounter != null) {
            lineFeedCountBeforeMatch = lfCounter.findLineFeedCountBeforeOffset(matchIndex)
            startOffset = deltaOffset + matchIndex + lineFeedCountBeforeMatch /* add as many \r as there were \n */

            val lineFeedCountBeforeEndOfMatch = lfCounter.findLineFeedCountBeforeOffset(matchIndex + value.length)
            val lineFeedCountInMatch = lineFeedCountBeforeEndOfMatch - lineFeedCountBeforeMatch
            endOffset = startOffset + value.length + lineFeedCountInMatch /* add as many \r as there were \n */
        } else {
            startOffset = deltaOffset + matchIndex
            endOffset = startOffset + value.length
        }

        val startPos = positionAt(startOffset)
        val endPos = positionAt(endOffset)
        Range(startPos, endPos)
    }

    // search by regex supports multiline mode
    fun findMatchesByMultiline(
        regex: Regex,
        searchRange: Range,
        limitResultCount: Int,
        isCancelled: () -> Boolean = { false }
    ) = lock.withLock {
        val pos = searchRange.startPosition
        val deltaOffset = offsetAt(pos.lineNumber, pos.column)

        // We always execute multiline search over the lines joined with \n
        // This makes it that \n will match the EOL for both CRLF and LF models
        // We compensate for offset errors in `getMultilineMatchRangeInternal`
        val text = getValueInRange(searchRange, "\n")
        val lfCounter = if (lineBreak == "\r\n") LineFeedCounter(text) else null

        val result = mutableListOf<Range>()
        var match = regex.find(text)
        while (match != null && result.size < limitResultCount && !isCancelled()) {
            result.add(
                getMultilineMatchRangeInternal(
                    text,
                    deltaOffset,
                    lfCounter,
                    match.range.first,
                    match.value
                )
            )
            // continue to find next
            match = match.next()
        }

        result.toList()
    }

    private fun TreeNode.findMatches(
        regex: Regex,
        startLineNumber: Int,
        startColumn: Int,
        startCursor: BufferCursor,
        endCursor: BufferCursor,
        limitResultCount: Int,
        result: MutableList<Range>,
        isCancelled: () -> Boolean = { false }
    ): Int = lock.withLock {
        val buffer = buffers[piece.bufferIndex]
        val startOffsetInBuffer = offsetInBuffer(piece.bufferIndex, piece.start)
        val start = offsetInBuffer(piece.bufferIndex, startCursor)
        val end = offsetInBuffer(piece.bufferIndex, endCursor)

        // let m: RegExpExecArray | null
        // Reset regex to search from the beginning
        val ret = BufferCursor(0, 0)
        val searchText = buffer.buffer.toString()

        // regex matcher
        var match = regex.find(searchText, start)

        while (match != null && !isCancelled()) {
            if (match.range.first >= end) return result.size

            positionInBuffer(match.range.first - startOffsetInBuffer, ret)
            val lineFeedCnt = getLineFeedCnt(piece.bufferIndex, startCursor, ret)
            val retStartColumn = if (ret.line == startCursor.line) {
                ret.column - startCursor.column + startColumn
            } else {
                ret.column + 1
            }
            val retEndColumn = retStartColumn + match.range.count()
            result.add(
                Range(
                    startLineNumber + lineFeedCnt,
                    retStartColumn,
                    startLineNumber + lineFeedCnt,
                    retEndColumn
                )
            )

            if (match.range.first + match.range.count() >= end) return result.size
            if (result.size >= limitResultCount) return result.size
            // search the next
            match = match.next()
        }

        return result.size
    }

    // search by regex, single line mode
    fun findMatchesLineByLine(
        regex: Regex,
        searchRange: Range,
        limitResultCount: Int,
        isCancelled: () -> Boolean
    ): List<Range> = lock.withLock {
        val result = mutableListOf<Range>()

        var startPosition = nodeAt(searchRange.startLine, searchRange.startColumn)
        if (startPosition === NullNodePosition) {
            return result
        }
        val endPosition = nodeAt(searchRange.endLine, searchRange.endColumn)
        if (endPosition === NullNodePosition) {
            return result
        }
        var start = startPosition.node.positionInBuffer(startPosition.remainder)
        val end = endPosition.node.positionInBuffer(endPosition.remainder)

        if (startPosition.node === endPosition.node) {
            startPosition.node.findMatches(
                regex = regex,
                startLineNumber = searchRange.startLine,
                startColumn = searchRange.startColumn,
                startCursor = start,
                endCursor = end,
                limitResultCount = limitResultCount,
                result = result,
                isCancelled = isCancelled
            )
            return result
        }

        var startLineNumber = searchRange.startLine

        var currentNode = startPosition.node
        while (currentNode !== endPosition.node) {
            val lineBreakCnt = getLineFeedCnt(currentNode.piece.bufferIndex, start, currentNode.piece.end)

            if (lineBreakCnt >= 1) {
                // last line break position
                val lineStarts = buffers[currentNode.piece.bufferIndex].lineStarts
                val startOffsetInBuffer = offsetInBuffer(currentNode.piece.bufferIndex, currentNode.piece.start)
                val nextLineStartOffset = lineStarts[start.line + lineBreakCnt]
                val startColumn = if (startLineNumber == searchRange.startLine) searchRange.startColumn else 1
                currentNode.findMatches(
                    regex = regex,
                    startLineNumber = startLineNumber,
                    startColumn = startColumn,
                    startCursor = start,
                    endCursor = currentNode.positionInBuffer(nextLineStartOffset - startOffsetInBuffer),
                    limitResultCount = limitResultCount,
                    result = result,
                    isCancelled = isCancelled
                )

                if (result.size >= limitResultCount) {
                    return result
                }

                startLineNumber += lineBreakCnt
            }

            val startColumn = if (startLineNumber == searchRange.startLine) searchRange.startColumn - 1 else 0
            // search for the remaining content
            if (startLineNumber == searchRange.endLine) {
                val searchText = this
                    .getLineContent(startLineNumber)
                    .substring(startColumn, searchRange.endColumn - 1)

                findMatchesInLine(
                    text = searchText,
                    regex = regex,
                    lineNumber = searchRange.endLine,
                    deltaOffset = startColumn,
                    result = result,
                    limitResultCount = limitResultCount
                )
                return result
            }

            findMatchesInLine(
                getLineContent(startLineNumber).substring(startColumn),
                regex,
                startLineNumber,
                startColumn,
                result,
                limitResultCount
            )

            if (result.size >= limitResultCount) return result

            startLineNumber++
            startPosition = nodeAt(startLineNumber, 1)
            currentNode = startPosition.node
            start = startPosition.node.positionInBuffer(startPosition.remainder)
        }

        if (startLineNumber == searchRange.endLine) {
            val startColumn = if (startLineNumber == searchRange.startLine) searchRange.startColumn - 1 else 0
            val searchText = getLineContent(startLineNumber).substring(startColumn, searchRange.endColumn - 1)
            findMatchesInLine(
                searchText,
                regex,
                searchRange.endLine,
                startColumn,
                result,
                limitResultCount
            )
            return result
        }

        val startColumn = if (startLineNumber == searchRange.startLine) searchRange.startColumn else 1
        endPosition.node.findMatches(
            regex = regex,
            startLineNumber = startLineNumber,
            startColumn = startColumn,
            startCursor = start,
            endCursor = end,
            limitResultCount = limitResultCount,
            result = result,
            isCancelled = isCancelled
        )

        result
    }

    private fun findMatchesInLine(
        text: String,
        regex: Regex,
        lineNumber: Int,
        deltaOffset: Int,
        result: MutableList<Range>,
        limitResultCount: Int
    ): Int {
        // Reset regex to search from the beginning
        var match = regex.find(text)
        while (match != null && result.size < limitResultCount) {
            result.add(
                Range(
                    startLine = lineNumber,
                    startColumn = match.range.first + 1 + deltaOffset,
                    endLine = lineNumber,
                    endColumn = match.range.first + 1 + match.range.count() + deltaOffset
                )
            )
            // search the next
            match = match.next()
        }
        return result.size
    }

    // search by word supports multiline mode
    fun findMatchesByWord(
        searchText: String,
        searchRange: Range,
        limitResultCount: Int,
        isCancelled: () -> Boolean = { false }
    ): List<Range> = lock.withLock {
        val result = mutableListOf<Range>()
        // lineFeed count
        val (lines, _, lastLineLength, _) = Strings.countLineBreaks(searchText)

        var deltaCount: Int
        var startOffset: Int
        var lineNumber = searchRange.startLine

        while (lineNumber + lines <= searchRange.endLine && !isCancelled()) {
            // get text from the range
            val text = getValueInRange(
                Range(
                    lineNumber,
                    1,
                    lineNumber + lines,
                    getLineLength(lineNumber + lines) + 1
                )
            )

            var lastMatchIndex = text.indexOf(searchText, 0)
            deltaCount = if (lastMatchIndex != -1) lines + 1 else 1

            while (lastMatchIndex != -1 && result.size < limitResultCount && !isCancelled()) {
                // match found, lines > 0 indicates that contains line feed
                startOffset = if (lines > 0) 0 else lastMatchIndex
                result.add(
                    Range(
                        startLine = lineNumber,
                        startColumn = lastMatchIndex + 1,
                        endLine = lineNumber + lines,
                        endColumn = startOffset + lastLineLength + 1
                    )
                )

                // the start index of next match
                lastMatchIndex = text.indexOf(searchText, lastMatchIndex + searchText.length)
            }

            // next start line number
            lineNumber += deltaCount
        }
        return result
    }

    fun insert(offset: Int, text: String, lineBreakNormalized: Boolean = false) = lock.withLock {
        this.lineBreakNormalized = this.lineBreakNormalized && lineBreakNormalized
        this.lastVisitedLine.lineNumber = 0
        this.lastVisitedLine.value = ""

        var value = text

        if (root !== Sentinel) {
            val (node, remainder, nodeStartOffset) = nodeAt(offset)
            val piece = node.piece
            val bufferIndex = piece.bufferIndex
            val insertPosInBuffer = node.positionInBuffer(remainder)
            if (node.piece.bufferIndex == 0 &&
                piece.end.line == lastChangeBufferPosition.line &&
                piece.end.column == lastChangeBufferPosition.column &&
                (nodeStartOffset + piece.length == offset) &&
                value.length < AverageBufferSize
            ) {
                // changed buffer
                node.append(value)
                computeBufferMetadata()
                return@insert
            }

            if (nodeStartOffset == offset) {
                node.insertContentLeft(value)
                searchCache.validate(offset)
            } else if (nodeStartOffset + node.piece.length > offset) {
                // we are inserting into the middle of a node.
                val nodesToDel = mutableListOf<TreeNode>()
                var newRightPiece = Piece(
                    bufferIndex = piece.bufferIndex,
                    start = insertPosInBuffer,
                    end = piece.end,
                    lineFeedCnt = getLineFeedCnt(piece.bufferIndex, insertPosInBuffer, piece.end),
                    length = offsetInBuffer(bufferIndex, piece.end) - offsetInBuffer(bufferIndex, insertPosInBuffer)
                )

                if (shouldCheckCRLF() && value.endWithCR()) {
                    val headOfRight = node.charCodeAt(remainder)
                    /** \n */
                    if (headOfRight == CharCode.LineFeed) {
                        val newStart = BufferCursor(newRightPiece.start.line + 1, 0)
                        newRightPiece = Piece(
                            bufferIndex = newRightPiece.bufferIndex,
                            start = newStart,
                            end = newRightPiece.end,
                            lineFeedCnt = getLineFeedCnt(
                                newRightPiece.bufferIndex,
                                newStart,
                                newRightPiece.end
                            ),
                            length = newRightPiece.length - 1
                        )

                        value += "\n"
                    }
                }

                // reuse node for content before insertion point.
                if (shouldCheckCRLF() && value.startWithLF()) {
                    val tailOfLeft = node.charCodeAt(remainder - 1)
                    /** \r */
                    if (tailOfLeft == CharCode.CarriageReturn) {
                        val previousPos = node.positionInBuffer(remainder - 1)
                        node.deleteTail(previousPos)
                        value = '\r' + value

                        if (node.piece.length == 0) {
                            nodesToDel.add(node)
                        }
                    } else {
                        node.deleteTail(insertPosInBuffer)
                    }
                } else {
                    node.deleteTail(insertPosInBuffer)
                }

                val newPieces = createNewPieces(value)

                if (newRightPiece.length > 0) {
                    rbInsertRight(node, newRightPiece)
                }

                var tmpNode = node
                for (k in 0..<newPieces.size) {
                    tmpNode = rbInsertRight(tmpNode, newPieces[k])
                }
                deleteNodes(nodesToDel)
            } else {
                node.insertContentRight(value)
            }
        } else {
            // insert new node
            val pieces = createNewPieces(value)
            var node = rbInsertLeft(NullTreeNode, pieces[0])

            for (k in 1..<pieces.size) {
                node = rbInsertRight(node, pieces[k])
            }
        }

        // todo, this is too brutal. Total line feed count should be updated the same way as lfLeft
        computeBufferMetadata()
    }

    fun delete(offset: Int, count: Int) = lock.withLock {
        this.lastVisitedLine.lineNumber = 0
        this.lastVisitedLine.value = ""

        if (count <= 0 || root === Sentinel) return@withLock

        val startPosition = nodeAt(offset)
        val endPosition = nodeAt(offset + count)
        val startNode = startPosition.node
        val endNode = endPosition.node

        if (startNode === endNode) {
            val startSplitPosInBuffer = startNode.positionInBuffer(startPosition.remainder)
            val endSplitPosInBuffer = startNode.positionInBuffer(endPosition.remainder)

            if (startPosition.nodeStartOffset == offset) {
                if (count == startNode.piece.length) {
                    val next = startNode.next()
                    rbDelete(startNode)
                    validateCRLFWithPrevNode(next)
                    computeBufferMetadata()
                    return@withLock
                }

                startNode.deleteHead(endSplitPosInBuffer)
                searchCache.validate(offset)
                validateCRLFWithNextNode(startNode)
                computeBufferMetadata()
                return@withLock
            }

            if (startPosition.nodeStartOffset + startNode.piece.length == offset + count) {
                startNode.deleteTail(startSplitPosInBuffer)
                validateCRLFWithNextNode(startNode)
                computeBufferMetadata()
                return@withLock
            }

            // delete content in the middle, this node will be splitted to nodes
            startNode.shrink(startSplitPosInBuffer, endSplitPosInBuffer)
            computeBufferMetadata()
            return@withLock
        }

        val nodesToDel = mutableListOf<TreeNode>()

        val startSplitPosInBuffer = startNode.positionInBuffer(startPosition.remainder)
        startNode.deleteTail(startSplitPosInBuffer)
        searchCache.validate(offset)
        if (startNode.piece.length == 0) {
            nodesToDel.add(startNode)
        }

        // update last touched node
        val endSplitPosInBuffer = endNode.positionInBuffer(endPosition.remainder)
        endNode.deleteHead(endSplitPosInBuffer)
        if (endNode.piece.length == 0) {
            nodesToDel.add(endNode)
        }

        // delete nodes in between
        var node = startNode.next()
        while (node !== Sentinel && node !== endNode) {
            nodesToDel.add(node)
            node = node.next()
        }

        val prev = if (startNode.piece.length == 0) startNode.prev() else startNode
        deleteNodes(nodesToDel)
        validateCRLFWithPrevNode(prev)
        computeBufferMetadata()
    }

    private fun TreeNode.deleteTail(pos: BufferCursor) {
        val originalLFCnt = piece.lineFeedCnt
        val originalEndOffset = offsetInBuffer(piece.bufferIndex, piece.end)

        val newEndOffset = offsetInBuffer(piece.bufferIndex, pos)
        val newLineFeedCnt = getLineFeedCnt(piece.bufferIndex, piece.start, pos)

        val lfDelta = newLineFeedCnt - originalLFCnt
        val sizeDelta = newEndOffset - originalEndOffset
        val newLength = piece.length + sizeDelta

        this.piece = Piece(piece.bufferIndex, piece.start, pos, newLineFeedCnt, newLength)

        updateMetadata(this, sizeDelta, lfDelta)
    }

    private fun TreeNode.deleteHead(pos: BufferCursor) {
        val originalLFCnt = piece.lineFeedCnt
        val originalStartOffset = offsetInBuffer(piece.bufferIndex, piece.start)

        val newLineFeedCnt = getLineFeedCnt(piece.bufferIndex, pos, piece.end)
        val newStartOffset = offsetInBuffer(piece.bufferIndex, pos)
        val lfDelta = newLineFeedCnt - originalLFCnt
        val sizeDelta = originalStartOffset - newStartOffset
        val newLength = piece.length + sizeDelta

        this.piece = Piece(piece.bufferIndex, pos, piece.end, newLineFeedCnt, newLength)

        updateMetadata(this, sizeDelta, lfDelta)
    }

    private fun TreeNode.insertContentLeft(text: String) = lock.withLock {
        // we are inserting content to the beginning of node
        val nodesToDel = mutableListOf<TreeNode>()
        var value = text

        if (shouldCheckCRLF() && value.endWithCR() && this.startWithLF()) {
            val newStart = BufferCursor(piece.start.line + 1, 0)
            val nPiece = Piece(
                bufferIndex = piece.bufferIndex,
                start = newStart,
                end = piece.end,
                lineFeedCnt = getLineFeedCnt(piece.bufferIndex, newStart, piece.end),
                length = piece.length - 1
            )

            this.piece = nPiece

            value += '\n'
            updateMetadata(this, -1, -1)

            if (piece.length == 0) {
                nodesToDel.add(this)
            }
        }

        val newPieces = createNewPieces(value)
        var newNode = rbInsertLeft(this, newPieces[newPieces.lastIndex])
        for (k in newPieces.size - 2 downTo 0) {
            newNode = rbInsertLeft(newNode, newPieces[k])
        }
        validateCRLFWithPrevNode(newNode)
        deleteNodes(nodesToDel)
    }

    private fun TreeNode.insertContentRight(text: String) = lock.withLock {
        // we are inserting to the right of this node.
        var value = text
        if (adjustCarriageReturnFromNext(value, this)) {
            // move \n to the new node.
            value += '\n'
        }

        val newPieces = createNewPieces(value)
        val newNode = rbInsertRight(this, newPieces[0])
        var tmpNode = newNode

        for (k in 1..<newPieces.size) {
            tmpNode = rbInsertRight(tmpNode, newPieces[k])
        }

        validateCRLFWithPrevNode(newNode)
    }

    private fun TreeNode.append(text: String) = lock.withLock {
        var value = text
        if (adjustCarriageReturnFromNext(value, this)) {
            value += '\n'
        }

        val hitCRLF = shouldCheckCRLF() && value.startWithLF() && this.endWithCR()
        val startOffset = buffers[0].buffer.length
        buffers[0].buffer.append(value)
        val lineStarts = value.computeLineStartOffsets()
        for (i in 0..<lineStarts.size) {
            lineStarts[i] += startOffset
        }
        if (hitCRLF) {
            val prevStartOffset = buffers[0].lineStarts[buffers[0].lineStarts.size - 2]
            // remove the last element
            buffers[0].lineStarts.removeLast()
            // lastChangeBufferPosition is already wrong
            lastChangeBufferPosition.line -= 1
            lastChangeBufferPosition.column = startOffset - prevStartOffset
        }

        buffers[0].lineStarts += lineStarts.slice(1..<lineStarts.size)
        val endIndex = buffers[0].lineStarts.size - 1
        val endColumn = buffers[0].buffer.length - buffers[0].lineStarts[endIndex]
        val newEnd = BufferCursor(endIndex, endColumn)
        val newLength = piece.length + value.length
        val oldLineFeedCnt = piece.lineFeedCnt
        val newLineFeedCnt = getLineFeedCnt(0, piece.start, newEnd)
        val lfDelta = newLineFeedCnt - oldLineFeedCnt

        piece = Piece(piece.bufferIndex, piece.start, newEnd, newLineFeedCnt, newLength)

        lastChangeBufferPosition = newEnd
        updateMetadata(this, value.length, lfDelta)
    }

    private fun TreeNode.shrink(start: BufferCursor, end: BufferCursor) = lock.withLock {
        val originalStartPos = piece.start
        val originalEndPos = piece.end

        // old piece, originalStartPos, start
        val oldLength = piece.length
        val oldLFCnt = piece.lineFeedCnt
        val newLineFeedCnt = getLineFeedCnt(piece.bufferIndex, piece.start, start)
        val newLength = offsetInBuffer(piece.bufferIndex, start) - offsetInBuffer(piece.bufferIndex, originalStartPos)

        this.piece = Piece(piece.bufferIndex, piece.start, start, newLineFeedCnt, newLength)

        updateMetadata(this, newLength - oldLength, newLineFeedCnt - oldLFCnt)

        // new right piece, end, originalEndPos
        val newPiece = Piece(
            bufferIndex = piece.bufferIndex,
            start = end,
            end = originalEndPos,
            lineFeedCnt = getLineFeedCnt(piece.bufferIndex, end, originalEndPos),
            length = offsetInBuffer(piece.bufferIndex, originalEndPos) - offsetInBuffer(piece.bufferIndex, end)
        )

        val newNode = rbInsertRight(this, newPiece)
        validateCRLFWithPrevNode(newNode)
    }

    private fun shouldCheckCRLF() = lock.withLock { !(lineBreakNormalized && lineBreak == "\n") }

    private fun CharSequence.startWithLF() = this.isNotEmpty() && this.first().code == CharCode.LineFeed

    private fun TreeNode.startWithLF(): Boolean = lock.withLock {
        if (this === Sentinel || piece.lineFeedCnt == 0) return false

        val lineStarts = buffers[piece.bufferIndex].lineStarts
        val line = piece.start.line
        val startOffset = lineStarts[line] + piece.start.column
        if (line == lineStarts.size - 1) {
            // last line, so there is no line feed at the end of this line
            return false
        }
        val nextLineOffset = lineStarts[line + 1]
        if (nextLineOffset > startOffset + 1) return false

        return buffers[piece.bufferIndex].buffer.codePointAt(startOffset) == CharCode.LineFeed
    }

    private fun CharSequence.endWithCR() = this.isNotEmpty() && this.last().code == CharCode.CarriageReturn

    private fun TreeNode.endWithCR(): Boolean = lock.withLock {
        if (this === Sentinel || piece.lineFeedCnt == 0) return false
        return charCodeAt(piece.length - 1) == 13
    }

    private fun validateCRLFWithPrevNode(nextNode: TreeNode) = lock.withLock {
        if (shouldCheckCRLF() && nextNode.startWithLF()) {
            val node = nextNode.prev()
            if (node.endWithCR()) fixCRLF(node, nextNode)
        }
    }

    private fun validateCRLFWithNextNode(node: TreeNode) = lock.withLock {
        if (shouldCheckCRLF() && node.endWithCR()) {
            val nextNode = node.next()
            if (nextNode.startWithLF()) fixCRLF(node, nextNode)
        }
    }

    private fun fixCRLF(prev: TreeNode, next: TreeNode) = lock.withLock {
        val nodesToDel = mutableListOf<TreeNode>()
        // update node
        val lineStarts = buffers[prev.piece.bufferIndex].lineStarts
        val newEnd = if (prev.piece.end.column == 0) {
            // it means, last line ends with \r, not \r\n
            BufferCursor(
                prev.piece.end.line - 1,
                lineStarts[prev.piece.end.line] - lineStarts[prev.piece.end.line - 1] - 1
            )
        } else {
            // \r\n
            BufferCursor(prev.piece.end.line, prev.piece.end.column - 1)
        }

        val prevNewLength = prev.piece.length - 1
        val prevNewLFCnt = prev.piece.lineFeedCnt - 1
        prev.piece = Piece(prev.piece.bufferIndex, prev.piece.start, newEnd, prevNewLFCnt, prevNewLength)

        updateMetadata(prev, -1, -1)
        if (prev.piece.length == 0) {
            nodesToDel.add(prev)
        }

        // update nextNode
        val newStart = BufferCursor(next.piece.start.line + 1, 0)
        val newLength = next.piece.length - 1
        val newLineFeedCnt = this.getLineFeedCnt(next.piece.bufferIndex, newStart, next.piece.end)
        next.piece = Piece(next.piece.bufferIndex, newStart, next.piece.end, newLineFeedCnt, newLength)

        updateMetadata(next, -1, -1)
        if (next.piece.length == 0) {
            nodesToDel.add(next)
        }

        // create new piece which contains \r\n
        val pieces = createNewPieces("\r\n")
        rbInsertRight(prev, pieces[0])
        // delete empty nodes
        for (node in nodesToDel) {
            rbDelete(node)
        }
    }

    private fun adjustCarriageReturnFromNext(value: String, node: TreeNode): Boolean = lock.withLock {
        if (shouldCheckCRLF() && value.endWithCR()) {
            val nextNode = node.next()
            if (nextNode.startWithLF()) {
                // move `\n` forward
                // value += '\n'

                if (nextNode.piece.length == 1) {
                    rbDelete(nextNode)
                } else {
                    val piece = nextNode.piece
                    val newStart = BufferCursor(piece.start.line + 1, 0)
                    val newLength = piece.length - 1
                    val newLineFeedCnt = getLineFeedCnt(piece.bufferIndex, newStart, piece.end)
                    nextNode.piece = Piece(piece.bufferIndex, newStart, piece.end, newLineFeedCnt, newLength)
                    updateMetadata(nextNode, -1, -1)
                }
                return true
            }
        }

        return false
    }

    private fun nodeAt(index: Int): NodePosition = lock.withLock {
        var x = root
        var offset = index
        val cache = searchCache[offset]
        if (cache != null) {
            return NodePosition(
                node = cache.node,
                remainder = offset - cache.nodeStartOffset,
                nodeStartOffset = cache.nodeStartOffset,
            )
        }

        var nodeStartOffset = 0
        while (x !== Sentinel) {
            if (x.sizeLeft > offset) {
                x = x.left
            } else if (x.sizeLeft + x.piece.length >= offset) {
                nodeStartOffset += x.sizeLeft
                val ret = NodePosition(
                    node = x,
                    remainder = offset - x.sizeLeft,
                    nodeStartOffset = nodeStartOffset
                )
                searchCache.insert(CacheEntry(x, nodeStartOffset, 0))
                return ret
            } else {
                offset -= x.sizeLeft + x.piece.length
                nodeStartOffset += x.sizeLeft + x.piece.length
                x = x.right
            }
        }

        return NullNodePosition
    }

    private fun nodeAt(row: Int, col: Int): NodePosition = lock.withLock {
        var x = this.root
        var nodeStartOffset = 0
        var lineNumber = row
        var column = col

        while (x !== Sentinel) {
            if (x.left !== Sentinel && x.lfLeft >= lineNumber - 1) {
                x = x.left
            } else if (x.lfLeft + x.piece.lineFeedCnt > lineNumber - 1) {
                val prevAccumualtedValue = x.getAccumulatedValue(lineNumber - x.lfLeft - 2)
                val accumulatedValue = x.getAccumulatedValue(lineNumber - x.lfLeft - 1)
                nodeStartOffset += x.sizeLeft

                return NodePosition(
                    node = x,
                    remainder = minOf(prevAccumualtedValue + column - 1, accumulatedValue),
                    nodeStartOffset = nodeStartOffset
                )
            } else if (x.lfLeft + x.piece.lineFeedCnt == lineNumber - 1) {
                val prevAccumulatedValue = x.getAccumulatedValue(lineNumber - x.lfLeft - 2)
                if (prevAccumulatedValue + column - 1 <= x.piece.length) {
                    return NodePosition(
                        node = x,
                        remainder = prevAccumulatedValue + column - 1,
                        nodeStartOffset = nodeStartOffset
                    )
                } else {
                    column -= x.piece.length - prevAccumulatedValue
                    break
                }
            } else {
                lineNumber -= x.lfLeft + x.piece.lineFeedCnt
                nodeStartOffset += x.sizeLeft + x.piece.length
                x = x.right
            }
        }

        // search in order, to find the node contains position.column
        x = x.next()
        while (x !== Sentinel) {
            if (x.piece.lineFeedCnt > 0) {
                val accumulatedValue = x.getAccumulatedValue(0)
                nodeStartOffset = offsetOfNode(x)
                return NodePosition(
                    node = x,
                    remainder = minOf(column - 1, accumulatedValue),
                    nodeStartOffset = nodeStartOffset
                )
            } else {
                if (x.piece.length >= column - 1) {
                    nodeStartOffset = offsetOfNode(x)
                    return NodePosition(
                        node = x,
                        remainder = column - 1,
                        nodeStartOffset = nodeStartOffset
                    )
                } else {
                    column -= x.piece.length
                }
            }

            x = x.next()
        }

        return NullNodePosition
    }

    private fun TreeNode.charCodeAt(offset: Int): Int {
        if (piece.lineFeedCnt < 1) return -1
        val buffer = buffers[piece.bufferIndex]
        val newOffset = offsetInBuffer(piece.bufferIndex, piece.start) + offset
        return buffer.buffer.codePointAt(newOffset)
    }

    private fun offsetOfNode(x: TreeNode): Int = lock.withLock {
        var node = x
        if (node === NullTreeNode) return 0

        var pos = node.sizeLeft
        while (node !== this.root) {
            if (node.parent.right === node) {
                pos += node.parent.sizeLeft + node.parent.piece.length
            }
            node = node.parent
        }
        return pos
    }

    private fun offsetInBuffer(bufferIndex: Int, cursor: BufferCursor): Int {
        val lineStarts = buffers[bufferIndex].lineStarts
        return lineStarts[cursor.line] + cursor.column
    }

    private fun TreeNode.iterate(action: (TreeNode) -> Boolean): Boolean {
        if (this === Sentinel) return action(this)
        if (!left.iterate(action)) return false
        if (!action(this)) return false
        return right.iterate(action)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun iterateTree(noinline action: (TreeNode) -> Boolean) = lock.withLock { root.iterate(action) }

    private fun TreeNode.content() = lock.withLock {
        if (this === Sentinel) return@withLock ""
        piece.content()
    }

    fun Piece.content() = lock.withLock {
        val buffer = buffers[bufferIndex]
        val startOffset = offsetInBuffer(bufferIndex, start)
        val endOffset = offsetInBuffer(bufferIndex, end)
        buffer.buffer.substring(startOffset, endOffset)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun getPieceContent(piece: Piece) = piece.content()

    private fun computeBufferMetadata() {
        var lfCnt = 1
        var len = 0
        var x = this.root
        while (x !== Sentinel) {
            lfCnt += x.lfLeft + x.piece.lineFeedCnt
            len += x.sizeLeft + x.piece.length
            x = x.right
        }

        this.lineCount = lfCnt
        this.length = len
        this.searchCache.validate(length)
    }

    /**
     * ```
     *     node              node
     *     /  \              /  \
     *    a   b    <----   a    b
     *                         /
     *                        z
     * ```
     */
    private fun rbInsertRight(node: TreeNode, p: Piece) = lock.withLock {
        val z = TreeNode(p, NodeColor.Red)
        z.left = Sentinel
        z.right = Sentinel
        z.parent = Sentinel
        z.sizeLeft = 0
        z.lfLeft = 0

        val x = this.root
        if (x === Sentinel) {
            this.root = z
            z.color = NodeColor.Black
        } else if (node.right === Sentinel) {
            node.right = z
            z.parent = node
        } else {
            val nextNode = node.right.leftest()
            nextNode.left = z
            z.parent = nextNode
        }

        fixInsert(z)
        z
    }

    /**
     * ```
     *     node              node
     *     /  \              /  \
     *    a   b     ---->   a    b
     *                       \
     *                        z
     * ```
     */
    private fun rbInsertLeft(node: TreeNode, p: Piece) = lock.withLock {
        val z = TreeNode(p, NodeColor.Red)
        z.left = Sentinel
        z.right = Sentinel
        z.parent = Sentinel
        z.sizeLeft = 0
        z.lfLeft = 0

        if (this.root === Sentinel) {
            this.root = z
            z.color = NodeColor.Black
        } else if (node.left === Sentinel) {
            node.left = z
            z.parent = node
        } else {
            val prevNode = node.left.rightest()
            prevNode.right = z
            z.parent = prevNode
        }

        fixInsert(z)
        z
    }

    private fun getContentOfSubTree(node: TreeNode): String = lock.withLock {
        buildString {
            node.iterate {
                //println("content: ${it.content().replace(Strings.newLine, "N")}")
                append(it.content())
                true
            }
        }
    }

    override fun hashCode(): Int {
        var result = lineCount
        result = 31 * result + length
        result = 31 * result + lineBreakLength
        result = 31 * result + lineBreakNormalized.hashCode()
        result = 31 * result + lock.hashCode()
        result = 31 * result + lineBreak.hashCode()
        result = 31 * result + lastChangeBufferPosition.hashCode()
        result = 31 * result + lastVisitedLine.hashCode()
        result = 31 * result + searchCache.hashCode()
        result = 31 * result + buffers.hashCode()
        result = 31 * result + root.hashCode()
        return result
    }
}
