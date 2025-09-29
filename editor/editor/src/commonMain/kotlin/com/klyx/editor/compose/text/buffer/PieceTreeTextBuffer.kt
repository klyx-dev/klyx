package com.klyx.editor.compose.text.buffer

import com.klyx.editor.compose.text.ApplyEditsResult
import com.klyx.editor.compose.text.ContentChange
import com.klyx.editor.compose.text.LineBreak
import com.klyx.editor.compose.text.Position
import com.klyx.editor.compose.text.Range
import com.klyx.editor.compose.text.ReverseEditOperation
import com.klyx.editor.compose.text.SingleEditOperation
import com.klyx.editor.compose.text.Strings
import com.klyx.editor.compose.text.TextChange
import com.klyx.editor.compose.text.ValidatedEditOperation
import com.klyx.editor.compose.text.codePointAt
import kotlinx.serialization.Serializable

@Serializable
class PieceTreeTextBuffer : CharSequence {
    internal val pieceTree: PieceTreeBase
    val bom: String

    var mightContainRtl = false
        private set

    var mightContainUnusualLineTerminators = false

    var mightContainNonBasicASCII = false
        private set

    internal constructor(
        chunks: List<TextBuffer>,
        lineBreak: String,
        lineBreakNormalized: Boolean,
        bom: String,
        isRtl: Boolean,
        isLineTerminators: Boolean,
        isBasicASCII: Boolean
    ) {
        pieceTree = PieceTreeBase(chunks, lineBreak, lineBreakNormalized)

        this.bom = bom
        mightContainRtl = isRtl
        mightContainUnusualLineTerminators = isLineTerminators
        mightContainNonBasicASCII = isBasicASCII
    }

    var lineBreak: String
        get() = pieceTree.lineBreak
        set(value) = pieceTree.setLineBreak(value)

    private val LineBreak.sequence
        get() = when (this) {
            LineBreak.TextDefined -> lineBreak
            LineBreak.LF -> "\n"
            LineBreak.CRLF -> "\r\n"
            else -> error("Unknown LineBreak preference")
        }

    override val length get() = pieceTree.length

    val lineCount get() = pieceTree.lineCount

    override operator fun get(index: Int): Char {
        return pieceTree.getCharCode(index).toChar()
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return getValueInRange(Range(positionAt(startIndex), positionAt(endIndex)))
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PieceTreeTextBuffer) return false

        if (bom != other.bom) return false
        if (lineBreak != other.lineBreak) return false
        return pieceTree == other.pieceTree
    }

    internal fun createSnapshot(preserveBom: Boolean): PieceTreeSnapshot {
        val bom = if (preserveBom) bom else ""
        return pieceTree.createSnapshot(bom)
    }

    fun insert(offset: Int, text: String) {

    }

    // read the all piece content
    fun readPiecesContent(callback: (String) -> Unit) {
        val snapshot = createSnapshot(false)
        var text = snapshot.read()
        while (text != null) {
            callback(text)
            text = snapshot.read()
        }
    }

    fun offsetAt(lineNumber: Int, column: Int) = pieceTree.offsetAt(lineNumber, column)
    fun offsetAt(position: Position) = pieceTree.offsetAt(position.lineNumber, position.column)
    fun positionAt(index: Int) = pieceTree.positionAt(index)

    fun rangeAt(start: Int, length: Int): Range {
        val end = start + length
        val startPosition = positionAt(start)
        val endPosition = positionAt(end)
        return Range(startPosition, endPosition)
    }

    fun getValueInRange(range: Range, lineBreak: LineBreak = LineBreak.TextDefined): String {
        return pieceTree.getValueInRange(range, lineBreak.sequence)
    }

    fun getValueInRange(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): String {
        return getValueInRange(Range(startLine, startColumn, endLine, endColumn))
    }

    fun getValueLengthInRange(range: Range, lineBreak: LineBreak = LineBreak.TextDefined): Int {
        if (range.isEmpty()) return 0

        if (range.startLine == range.endLine) {
            return range.endColumn - range.startColumn
        }

        val startOffset = offsetAt(range.startLine, range.startColumn)
        val endOffset = offsetAt(range.endLine, range.endColumn)
        return endOffset - startOffset
    }

    fun getCharacterCountInRange(range: Range, lineBreak: LineBreak = LineBreak.TextDefined): Int {
        if (mightContainNonBasicASCII) {
            // we must count by iterating
            var result = 0

            val fromLine = range.startLine
            val toLine = range.endLine
            for (lineNumber in fromLine..toLine) {
                val lineContent = getLineContent(lineNumber)
                val fromOffset = (if (lineNumber == fromLine) range.startColumn - 1 else 0)
                val toOffset = (if (lineNumber == toLine) range.endColumn - 1 else lineContent.length)
                var offset = fromOffset

                while (offset < toOffset) {
                    if (Strings.isHighSurrogate(lineContent.codePointAt(offset))) {
                        result += 1
                        offset += 1
                    } else {
                        result += 1
                    }
                    offset++
                }
            }

            result += lineBreak.sequence.length * (toLine - fromLine)

            return result
        }

        return getValueLengthInRange(range, lineBreak)
    }

    fun getLineContent(lineNumber: Int) = pieceTree.getLineContent(lineNumber)
    fun getLineContentWithLineBreak(lineNumber: Int) = pieceTree.getLineContentWithLineBreak(lineNumber)

    fun getLineCharCode(lineNumber: Int, index: Int) = pieceTree.getLineCharCode(lineNumber, index)
    fun getCharCode(index: Int) = pieceTree.getCharCode(index)
    fun getLineLength(lineNumber: Int) = pieceTree.getLineLength(lineNumber)

    @Suppress("unused")
    fun getLineMinColumn(lineNumber: Int) = 1
    fun getLineMaxColumn(lineNumber: Int) = getLineLength(lineNumber) + 1

    fun getFirstNonWhitespaceColumn(lineNumber: Int): Int {
        val result = Strings.firstNonWhitespaceIndex(getLineContent(lineNumber))
        if (result == -1) return 0
        return result + 1
    }

    fun getLastNonWhitespaceColumn(lineNumber: Int): Int {
        val result = Strings.lastNonWhitespaceIndex(getLineContent(lineNumber))
        if (result == -1) return 0
        return result + 2
    }

    fun getNearestChunk(index: Int) = pieceTree.getNearestChunk(index)

    fun lines() = pieceTree.lines()

    // search text by regex
    fun find(
        regex: Regex,
        searchRange: Range,
        limitResultCount: Int,
        isCancelled: () -> Boolean
    ) = if (regex.options.contains(RegexOption.MULTILINE)) {
        // multi line mode
        pieceTree.findMatchesByMultiline(regex, searchRange, limitResultCount, isCancelled)
    } else {
        // single line mode
        pieceTree.findMatchesLineByLine(regex, searchRange, limitResultCount, isCancelled)
    }

    // search text by word
    fun find(
        searchText: String,
        searchRange: Range,
        limitResultCount: Int,
        isCancelled: () -> Boolean
    ) = pieceTree.findMatchesByWord(searchText, searchRange, limitResultCount, isCancelled)

    // text edits
    fun applyEdits(
        rawOperations: List<SingleEditOperation>,
        recordTrimAutoWhitespace: Boolean = false,
        computeUndoEdits: Boolean = false
    ): ApplyEditsResult {
        var mightContainRTL = this.mightContainRtl
        var mightContainUnusualLineTerminators = this.mightContainUnusualLineTerminators
        var mightContainNonBasicASCII = this.mightContainNonBasicASCII
        var canReduceOperations = true

        var operations = mutableListOf<ValidatedEditOperation>()
        for (i in 0..<rawOperations.size) {
            val op = rawOperations[i]
            if (canReduceOperations && i < 1000/*op.isTracked*/) {
                canReduceOperations = false
            }

            var validText = ""
            // compute text eol
            val (eolCount, firstLineLength, lastLineLength, eol) = Strings.countLineBreaks(op.text)

            val validatedRange = op.range
            op.text?.let {
                var textMightContainNonBasicASCII = true
                if (!mightContainNonBasicASCII) {
                    textMightContainNonBasicASCII = !Strings.isBasicASCII(it)
                    mightContainNonBasicASCII = textMightContainNonBasicASCII
                }
                if (!mightContainRTL && textMightContainNonBasicASCII) {
                    // check if the new inserted text contains RTL
                    mightContainRTL = Strings.containsRTL(it)
                }
                if (!mightContainUnusualLineTerminators && textMightContainNonBasicASCII) {
                    // check if the new inserted text contains unusual line terminators
                    mightContainUnusualLineTerminators = Strings.containsUnusualLineTerminators(it)
                }

                val expectedStrLineBreak = if (lineBreak == "\r\n") LineBreak.CRLF else LineBreak.LF
                validText = if (eol == LineBreak.TextDefined || eol == expectedStrLineBreak) {
                    op.text
                } else {
                    op.text.replace(Strings.newLine, lineBreak)
                }
            }

            operations.add(
                ValidatedEditOperation(
                    sortIndex = i,
                    identifier = op.identifier,
                    range = validatedRange,
                    rangeOffset = offsetAt(validatedRange.startLine, validatedRange.startColumn),
                    rangeLength = getValueLengthInRange(validatedRange),
                    text = validText,
                    eolCount = eolCount,
                    firstLineLength = firstLineLength,
                    lastLineLength = lastLineLength,
                    forceMoveMarkers = op.forceMoveMarkers,
                    isAutoWhitespaceEdit = op.isAutoWhitespaceEdit
                )
            )
        }

        // sort operations ascending
        operations.sortWith(::sortOpsAscending)

        var hasTouchingRanges = false
        // operations.size must be > 1
        for (i in 0 until operations.size - 1) {
            val rangeEnd = operations[i].range.endPosition
            val nextRangeStart = operations[i + 1].range.startPosition

            if (nextRangeStart.isBeforeOrEqual(rangeEnd)) {
                if (nextRangeStart.isBefore(rangeEnd)) {
                    // overlapping ranges
                    throw Error("Overlapping ranges are not allowed!")
                }
                hasTouchingRanges = true
            }
        }

        if (canReduceOperations) {
            operations = operations.reduce()
        }

        // Delta encode operations
        val reverseRanges = when {
            (computeUndoEdits || recordTrimAutoWhitespace) -> operations.getInverseEditRanges()
            else -> listOf()
        }

        // {lineNumber: number, oldContent: string}
        val newTrimAutoWhitespaceCandidates = mutableListOf<Pair<Int, String>>()
        if (recordTrimAutoWhitespace) {
            for (i in 0..<operations.size) {
                val op = operations[i]
                val reverseRange = reverseRanges[i]

                if (op.isAutoWhitespaceEdit && op.range.isEmpty()) {
                    // Record already the future line numbers that might be auto whitespace removal
                    // candidates on next edit
                    for (lineNumber in reverseRange.startLine..reverseRange.endLine) {
                        var currentLineContent = ""
                        if (lineNumber == reverseRange.startLine) {
                            currentLineContent = getLineContent(op.range.startLine)
                            if (Strings.firstNonWhitespaceIndex(currentLineContent) != -1) {
                                continue
                            }
                        }
                        newTrimAutoWhitespaceCandidates.add(Pair(lineNumber, currentLineContent))
                    }
                }
            }
        }

        var reverseOperations: MutableList<ReverseEditOperation>? = null
        if (computeUndoEdits) {
            var reverseRangeDeltaOffset = 0
            reverseOperations = mutableListOf()
            for (i in 0..<operations.size) {
                val op = operations[i]
                val reverseRange = reverseRanges[i]
                val bufferText = getValueInRange(op.range)
                val reverseRangeOffset = op.rangeOffset + reverseRangeDeltaOffset
                reverseRangeDeltaOffset += (op.text!!.length - bufferText.length)

                reverseOperations.add(
                    ReverseEditOperation(
                        sortIndex = op.sortIndex,
                        identifier = op.identifier,
                        range = reverseRange,
                        text = bufferText,
                        textChange = TextChange(op.rangeOffset, bufferText, reverseRangeOffset, op.text)
                    )
                )
            }

            // Can only sort reverse operations when the order is not significant
            if (!hasTouchingRanges) {
                // reverseOperations.sort((a, b) => a.sortIndex - b.sortIndex)
                reverseOperations.sortBy { it.sortIndex }
            }
        }

        this.mightContainRtl = mightContainRTL
        this.mightContainUnusualLineTerminators = mightContainUnusualLineTerminators
        this.mightContainNonBasicASCII = mightContainNonBasicASCII

        val contentChanges = operations.applyEdits()

        var trimAutoWhitespaceLineNumbers: MutableList<Int>? = null
        if (recordTrimAutoWhitespace && newTrimAutoWhitespaceCandidates.isNotEmpty()) {
            // sort line numbers auto whitespace removal candidates for next edit descending
            // newTrimAutoWhitespaceCandidates.sort((a, b) => b.lineNumber - a.lineNumber)
            newTrimAutoWhitespaceCandidates.sortByDescending { it.first } // first => lineNumber

            trimAutoWhitespaceLineNumbers = mutableListOf()
            for (i in 0..<newTrimAutoWhitespaceCandidates.size) {
                val (lineNumber, oldContent) = newTrimAutoWhitespaceCandidates[i]
                val (prevLineNumber, _) = newTrimAutoWhitespaceCandidates[i - 1]
                if (i > 0 && prevLineNumber == lineNumber) {
                    // Do not have the same line number twice
                    continue
                }

                val lineContent = getLineContent(lineNumber)

                if (lineContent.isEmpty() ||
                    lineContent == oldContent ||
                    Strings.firstNonWhitespaceIndex(lineContent) != -1
                ) {
                    continue
                }

                trimAutoWhitespaceLineNumbers.add(lineNumber)
            }
        }

        // onDidChangeContent.fire()

        return ApplyEditsResult(contentChanges, reverseOperations, trimAutoWhitespaceLineNumbers)
    }

    internal fun MutableList<ValidatedEditOperation>.applyEdits(): List<ContentChange> {
        this.sortWith(::sortOpsDescending)

        val contents = mutableListOf<ContentChange>()
        for (op in this) {
            val startLine = op.range.startLine
            val startColumn = op.range.startColumn
            val endLine = op.range.endLine
            val endColumn = op.range.endColumn

            // deletion
            pieceTree.delete(op.rangeOffset, op.rangeLength)

            // insertion
            if (op.text != null && op.text.isNotEmpty()) {
                pieceTree.insert(op.rangeOffset, op.text, true)
            }

            val contentChangeRange = Range(startLine, startColumn, endLine, endColumn)
            contents.add(
                ContentChange(
                    range = contentChangeRange,
                    rangeLength = op.rangeLength,
                    text = op.text,
                    rangeOffset = op.rangeOffset,
                    forceMoveMarkers = op.forceMoveMarkers
                )
            )
        }

        return contents
    }

    /**
     * Transform operations such that they represent the same logic edit, but that they also do not
     * cause OOM crashes.
     */
    private fun MutableList<ValidatedEditOperation>.reduce(): MutableList<ValidatedEditOperation> {
        if (size < 1000) {
            // We know from empirical testing that a thousand edits work fine regardless of their
            // shape.
            return this
        }

        // At one point, due to how events are emitted and how each operation is handled,
        // some operations can trigger a high amount of temporary string allocations,
        // that will immediately get edited again.
        // e.g. a formatter inserting ridiculous ammounts of \n on a model with a single line
        // Therefore, the strategy is to collapse all the operations into a huge single edit
        // operation
        return mutableListOf(this.toSingleEditOperation())
    }

    fun List<ValidatedEditOperation>.toSingleEditOperation(): ValidatedEditOperation {
        var forceMoveMarkers = false
        val firstEditRange = first().range
        val lastEditRange = last().range
        val entireEditRange = Range(
            firstEditRange.startLine,
            firstEditRange.startColumn,
            lastEditRange.endLine,
            lastEditRange.endColumn
        )
        var lastEndLineNumber = firstEditRange.startLine
        var lastEndColumn = firstEditRange.startColumn

        val result = StringBuilder()

        for (op in this) {
            val range = op.range

            forceMoveMarkers = forceMoveMarkers || op.forceMoveMarkers

            // (1) -- Push old text
            result.append(
                getValueInRange(
                    Range(lastEndLineNumber, lastEndColumn, range.startLine, range.startColumn)
                )
            )

            // (2) -- Push new text
            op.text?.let {
                result.append(op.text)
            }

            lastEndLineNumber = range.endLine
            lastEndColumn = range.endColumn
        }

        val text = result.toString()
        val (eolCount, firstLineLength, lastLineLength, _) = Strings.countLineBreaks(text)

        return ValidatedEditOperation(
            sortIndex = 0,
            identifier = first().identifier,
            range = entireEditRange,
            rangeOffset = offsetAt(entireEditRange.startLine, entireEditRange.startColumn),
            rangeLength = getValueLengthInRange(entireEditRange, LineBreak.TextDefined),
            text = text,
            eolCount = eolCount,
            firstLineLength = firstLineLength,
            lastLineLength = lastLineLength,
            forceMoveMarkers = forceMoveMarkers,
            isAutoWhitespaceEdit = false
        )
    }

    override fun hashCode(): Int {
        var result = mightContainRtl.hashCode()
        result = 31 * result + mightContainUnusualLineTerminators.hashCode()
        result = 31 * result + mightContainNonBasicASCII.hashCode()
        result = 31 * result + pieceTree.hashCode()
        result = 31 * result + bom.hashCode()
        result = 31 * result + length
        result = 31 * result + lineBreak.hashCode()
        return result
    }

    companion object {
        private fun getInverseEditRange(range: Range, text: String): Range {
            val startLine = range.startLine
            val startColumn = range.startColumn

            val (eolCount, firstLineLength, lastLineLength, _) = Strings.countLineBreaks(text)

            val inverseRange = if (text.isNotEmpty()) {
                // the operation inserts something
                val lineCount = eolCount + 1

                if (lineCount == 1) {
                    // single line insert
                    Range(startLine, startColumn, startLine, startColumn + firstLineLength)
                } else {
                    // multi line insert
                    Range(startLine, startColumn, startLine + lineCount - 1, lastLineLength + 1)
                }
            } else {
                // There is nothing to insert
                Range(startLine, startColumn, startLine, startColumn)
            }

            return inverseRange
        }

        /** Assumes the list items are validated and sorted ascending */
        internal fun List<ValidatedEditOperation>.getInverseEditRanges(): List<Range> {
            val result = mutableListOf<Range>()

            var prevOpEndLineNumber = 0
            var prevOpEndColumn = 0
            var prevOp: ValidatedEditOperation? = null
            for (op in this) {
                val startLineNumber: Int
                val startColumn: Int

                if (prevOp != null) {
                    if (prevOp.range.endLine == op.range.startLine) {
                        startLineNumber = prevOpEndLineNumber
                        startColumn = prevOpEndColumn + (op.range.startColumn - prevOp.range.endColumn)
                    } else {
                        startLineNumber = prevOpEndLineNumber + (op.range.startLine - prevOp.range.endLine)
                        startColumn = op.range.startColumn
                    }
                } else {
                    startLineNumber = op.range.startLine
                    startColumn = op.range.startColumn
                }

                val resultRange: Range

                if (op.text!!.isNotEmpty()) {
                    // the operation inserts something
                    val lineCount = op.eolCount + 1

                    if (lineCount == 1) {
                        // single line insert
                        resultRange =
                            Range(startLineNumber, startColumn, startLineNumber, startColumn + op.firstLineLength)
                    } else {
                        // multi line insert
                        resultRange =
                            Range(startLineNumber, startColumn, startLineNumber + lineCount - 1, op.lastLineLength + 1)
                    }
                } else {
                    // There is nothing to insert
                    resultRange = Range(startLineNumber, startColumn, startLineNumber, startColumn)
                }

                prevOpEndLineNumber = resultRange.endLine
                prevOpEndColumn = resultRange.endColumn

                result.add(resultRange)
                prevOp = op
            }

            return result
        }

        private fun sortOpsAscending(a: ValidatedEditOperation, b: ValidatedEditOperation): Int {
            val r = Range.compareRangesUsingEnds(a.range, b.range)
            return if (r == 0) a.sortIndex - b.sortIndex else r
        }

        private fun sortOpsDescending(a: ValidatedEditOperation, b: ValidatedEditOperation): Int {
            val r = Range.compareRangesUsingEnds(a.range, b.range)
            return if (r == 0) b.sortIndex - a.sortIndex else -r
        }
    }
}
