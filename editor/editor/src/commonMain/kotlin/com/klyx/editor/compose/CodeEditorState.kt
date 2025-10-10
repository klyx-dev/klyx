package com.klyx.editor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import com.klyx.core.file.KxFile
import com.klyx.core.getOrThrow
import com.klyx.editor.compose.renderer.LineDividerWidth
import com.klyx.editor.compose.renderer.LinePadding
import com.klyx.editor.compose.renderer.SpacingAfterLineBackground
import com.klyx.editor.compose.scroll.ScrollState
import com.klyx.editor.compose.text.ContentChange
import com.klyx.editor.compose.text.Cursor
import com.klyx.editor.compose.text.Range
import com.klyx.editor.compose.text.ReverseEditOperation
import com.klyx.editor.compose.text.Selection
import com.klyx.editor.compose.text.SingleEditOperation
import com.klyx.editor.compose.text.Strings
import com.klyx.editor.compose.text.TextAction
import com.klyx.editor.compose.text.TextChange
import com.klyx.editor.compose.text.asCursor
import com.klyx.editor.compose.text.buffer.EmptyTextBuffer
import com.klyx.editor.compose.text.buffer.PieceTreeTextBuffer
import com.klyx.editor.compose.text.buffer.toTextBuffer
import com.klyx.editor.compose.text.toPosition
import com.klyx.editor.compose.text.toSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.log10

@Stable
class CodeEditorState internal constructor(
    val buffer: PieceTreeTextBuffer = EmptyTextBuffer,
    internal val editable: Boolean,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private val charBreakIterator = BreakIterator.makeCharacterInstance()

    internal var fontSize: TextUnit = TextUnit.Unspecified
        set(value) {
            val newValue = maxOf(minOf(value.value, 40f), 7f)
            field = newValue.sp
        }
    internal var fontFamily: FontFamily = FontFamily.Monospace

    internal var colorScheme = DefaultEditorColorScheme
    internal var textMeasurer: Option<TextMeasurer> = none()
    internal val textStyle
        get() = TextStyle.Default.copy(
            fontFamily = fontFamily,
            fontSize = fontSize,
            color = colorScheme.foreground,
        )

    val lineBreak get() = buffer.lineBreak
    val lineCount get() = buffer.lineCount
    val bufferLength get() = buffer.length

    var cursor by mutableStateOf(Cursor.Start)
    var selection by mutableStateOf(Selection.Zero)

    val selectionStart get() = cursorAt(selection.min)
    val selectionEnd get() = cursorAt(selection.max)
    val cursorOffset get() = offsetAt(cursor)

    private val Int.count get() = log10(this.toDouble()).toInt() + 1

    internal val tabWidth get() = textWidth("\t")
    internal val lineHeight get() = measureText("W").map { it.size.height }.getOrThrow()

    internal var invalidateDrawCallback: InvalidateDrawCallback = {}
    internal var cursorAlpha by mutableStateOf(1f)
    internal val scrollState = ScrollState(0f, 0f)
    internal val undoRedoManager = UndoRedoManager()

    internal var viewportSize = Size.Zero
    internal var showLineNumber: Boolean = true
    internal var pinLineNumber: Boolean = true

    private val mergeTextChanges = mutableListOf<TextChange>()
    private var mergeJob: Job? = null

    var scrollX: Float
        get() = scrollState.offsetX
        set(value) = scrollTo(value, scrollState.offsetY)

    var scrollY: Float
        get() = scrollState.offsetY
        set(value) = scrollTo(scrollState.offsetX, value)

    internal val maxScrollX: Float
        get() {
            val avgCharWidth = textWidth("W").toFloat()
            val maxLineLength = (1..lineCount).maxOfOrNull { getLineLength(it) } ?: 0
            val estimatedWidth = maxLineLength * avgCharWidth

            return (estimatedWidth + getStartSpacing() + getEndSpacing() - viewportSize.width)
                .coerceAtLeast(0f).unaryMinus()
        }

    internal val maxScrollY: Float
        get() = ((lineCount * lineHeight) - viewportSize.height / 2f).coerceAtLeast(0f).unaryMinus()

    internal val maxScrollOffset get() = Offset(maxScrollX, maxScrollY)

    internal fun insert(text: String, range: Range) {
        if (editable) {
            applyEdits(listOf(SingleEditOperation(range, text)))
        }
    }

    internal fun delete(range: Range) {
        if (editable && range.isNotEmpty()) {
            applyEdits(listOf(SingleEditOperation(range, null)))
        }
    }

    fun insert(text: String, selection: Selection = this.selection) {
        if (selection.collapsed) {
            insert(text, TextAction.Insert.range())
        } else {
            insert(text, selection.asRange())
            collapseSelection()
        }
    }

    fun delete(selection: Selection = this.selection) {
        if (selection.collapsed) {
            delete(TextAction.Delete.range())
        } else {
            delete(selection.asRange())
            collapseSelection()
        }
    }

    internal fun Selection.asRange() = Range(startCursor().toPosition(), endCursor().toPosition())
    internal fun Selection.startCursor() = cursorAt(start)
    internal fun Selection.endCursor() = cursorAt(end)

    fun select(anchor: Int = selection.start, active: Int = cursorOffset) {
        selection = Selection(anchor.coerceAtLeast(0), active.coerceAtLeast(0))
    }

    fun select(range: IntRange) {
        selection = range.toSelection()
    }

    fun collapseSelection() {
        selection = Selection(cursorOffset)
    }

    fun moveCursor(line: Int, column: Int = 0, select: Boolean = false) {
        val l = line.coerceAtMost(lineCount)
        val c = column.coerceAtMost(getLineLength(l))
        cursor = Cursor(l, c)
        if (select) select() else collapseSelection()
        scope.launch { ensureCursorVisible() }
        invalidateDraw()
    }

    fun moveCursor(newCursor: Cursor, select: Boolean = false) {
        moveCursor(newCursor.line, newCursor.column, select = select)
    }

    fun moveCursor(direction: Direction, select: Boolean = false) {
        val line = cursor.line
        val column = cursor.column

        when (direction) {
            Direction.Left -> {
                if (column > 0) {
                    // Move one character left, accounting for grapheme clusters
                    val text = getLine(line)
                    val prevEnd = column - 1
                    val prevStart = getCharStart(text, prevEnd)
                    cursor = Cursor(line, prevStart)
                } else if (line > 1) {
                    // Move to end of previous line
                    val prevLineLength = getLineLength(line - 1)
                    cursor = Cursor(line - 1, prevLineLength)
                }
            }

            Direction.Right -> {
                val lineText = getLine(line)
                if (column < lineText.length) {
                    // Move one character right, respecting multibyte chars
                    val nextStart = getCharEnd(lineText, column)
                    cursor = Cursor(line, nextStart)
                } else if (line < lineCount) {
                    // Move to start of next line
                    cursor = Cursor(line + 1, 0)
                }
            }

            Direction.Up -> {
                if (line > 1) {
                    val prevLineLength = getLineLength(line - 1)
                    val newColumn = column.coerceAtMost(prevLineLength)
                    cursor = Cursor(line - 1, newColumn)
                }
            }

            Direction.Down -> {
                if (line < lineCount) {
                    val nextLineLength = getLineLength(line + 1)
                    val newColumn = column.coerceAtMost(nextLineLength)
                    cursor = Cursor(line + 1, newColumn)
                }
            }
        }

        if (select) select() else collapseSelection()

        scope.launch { ensureCursorVisible() }
        invalidateDraw()
    }

    fun getCharStart(text: String, endOffset: Int): Int {
        return with(charBreakIterator) {
            setText(text)
            following(endOffset)
            previous()
        }
    }

    fun getCharEnd(text: String, startOffset: Int): Int {
        return with(charBreakIterator) {
            setText(text)
            // requires startOffset < text.length
            // end boundary
            following(startOffset)
        }
    }

    private suspend fun ensureCursorVisible(padding: Float = 48f, smooth: Boolean = true) {
        val cursorLine = cursor.line
        val cursorColumn = cursor.column

        val leftOffset = getContentLeftOffset()

        // cursor X position relative to content area (not viewport)
        val cursorX = getTextWidths(getLine(cursorLine))
            .take(maxOf(0, cursorColumn))
            .sum()

        val cursorY = (cursorLine - 1) * lineHeight

        val visibleXStart = -scrollX
        val visibleYStart = -scrollY
        val visibleXEnd = visibleXStart + viewportSize.width - leftOffset
        val visibleYEnd = visibleYStart + viewportSize.height

        var targetX = -scrollX
        var targetY = -scrollY

        // check if cursor is outside visible X range
        if (cursorX < visibleXStart + padding) {
            targetX = (cursorX - padding).coerceAtLeast(0f)
        } else if (cursorX > visibleXEnd - padding) {
            targetX = (cursorX - (viewportSize.width - leftOffset) + padding).coerceAtLeast(0f)
        }

        // check if cursor is outside visible Y range
        if (cursorY < visibleYStart + padding) {
            targetY = (cursorY - padding).coerceAtLeast(0f)
        } else if (cursorY + lineHeight > visibleYEnd - padding) {
            targetY = (cursorY - viewportSize.height + lineHeight + padding).coerceAtLeast(0f)
        }

        if (smooth) {
            smoothScrollTo(-targetX, -targetY, duration = 180)
        } else {
            scrollTo(-targetX, -targetY)
        }
    }

    internal fun getContentLeftOffset(): Float {
        val lineNumberWidth = if (showLineNumber) getLineNumberWidth().toFloat() else 0f
        return lineNumberWidth + LinePadding + LineDividerWidth + SpacingAfterLineBackground
    }

    internal fun calculateCursorPositionFromScreenOffset(offset: Offset): Cursor {
        val lineNumber = ((offset.y - scrollState.offsetY) / lineHeight).toInt() + 1
        val clampedLine = lineNumber.coerceIn(1, lineCount)
        val leftOffset = getContentLeftOffset()

        val adjustedX = (offset.x - leftOffset - scrollState.offsetX).coerceAtLeast(0f)

        val line = getLine(clampedLine)
        if (line.isEmpty()) return Cursor(clampedLine, 0)

        val widths = getTextWidths(line)
        var currentWidth = 0f
        var column = 0

        for (i in widths.indices) {
            val charWidth = widths[i]
            // check if click is in the first half or second half of character
            if (adjustedX < currentWidth + charWidth / 2) {
                break
            }
            currentWidth += charWidth
            column++
        }

        return Cursor(clampedLine, column)
    }

    internal fun getTextInRange(range: Range) = buffer.getValueInRange(range)

    private fun TextAction.range() = when (this) {
        TextAction.Insert -> Range(cursor.toPosition())
        TextAction.Delete -> {
            if (cursor.line == 1 && cursor.column == 0) {
                Range(cursor.toPosition())
            } else if (cursor.column == 0 && cursor.line > 1) {
                // delete the line feed (\n)
                Range(
                    startLine = cursor.line - 1,
                    startColumn = buffer.getLineMaxColumn(cursor.line - 1),
                    endLine = cursor.line,
                    endColumn = cursor.column + 1
                )
            } else {
                Range(cursor.line, 1, cursor.line, cursor.column + 1).apply {
                    // reset range start column, may contains unicode characters
                    startColumn = getCharStart(getTextInRange(this), endColumn) + 1
                }
            }
        }
    }

    private fun applyEdits(
        operations: List<SingleEditOperation>,
        computeUndoEdits: Boolean = true
    ) {
        beforeTextChanged(operations)
        val result = buffer.applyEdits(operations, false, computeUndoEdits)

        var lastLineNumber = 1
        var lastColumn = 0

        result.changes.forEachIndexed { index, change ->
            // compute the changed lines
            val (insertingLinesCnt, _, lastLineLength, _) = Strings.countLineBreaks(change.text!!)
            val deletingLinesCnt = change.range.endLine - change.range.startLine

            val finalLineNumber = change.range.startLine + insertingLinesCnt
            var finalColumn = change.range.endColumn

            finalColumn = if (change.text.isNotEmpty()) {
                // insert or replace text (insert lines 0)
                when (insertingLinesCnt) {
                    0 -> change.range.startColumn + lastLineLength
                    else -> lastLineLength + 1
                }
            } else {
                // delete text
                change.range.startColumn
            }

            // batch edits
            if (index == 0) {
                lastLineNumber = finalLineNumber
                lastColumn = finalColumn
            }

            onTextChanged(
                range = change.range,
                rangeOffset = change.rangeOffset,
                insertedLinesCnt = insertingLinesCnt,
                insertedTextLength = change.text.length,
                deletedLinesCnt = deletingLinesCnt,
                deletedTextLength = change.rangeLength,
                finalLineNumber = finalLineNumber,
                finalColumn = finalColumn
            )
        }

        if (computeUndoEdits) {
            scope.launch { pushEdits(result.reverseEdits) }
        }

        afterTextChanged(result.changes, lastLineNumber, lastColumn)
    }

    private suspend fun pushEdits(
        reverseEdits: List<ReverseEditOperation>?,
        delay: Long = 500L
    ) = mutex.withLock {
        undoRedoManager.clearRedo()
        onMergeTextChangesBefore()

        reverseEdits?.let { operations ->
            mergeTextChanges.addAll(
                TextChange.compressConsecutiveTextChanges(
                    mergeTextChanges,
                    operations.map { it.textChange }
                )
            )

            delay(delay)

            mergeJob = coroutineScope {
                launch {
                    undoRedoManager.push(mergeTextChanges)
                    mergeTextChanges.clear()
                    onMergeTextChangesAfter()
                }
            }
        }
    }

    private suspend fun onMergeTextChangesBefore() {}
    private suspend fun onMergeTextChangesAfter() {}

    private fun afterTextChanged(
        changes: List<ContentChange>,
        lastLineNumber: Int,
        lastColumn: Int
    ) {
        cursor = Cursor(lastLineNumber, lastColumn - 1)
        scope.launch { ensureCursorVisible(smooth = false) }
        invalidateDraw()
    }

    private fun beforeTextChanged(operations: List<SingleEditOperation>) {
        mergeJob?.cancel()
    }

    private fun onTextChanged(
        range: Range,
        rangeOffset: Int,
        insertedLinesCnt: Int,
        insertedTextLength: Int,
        deletedLinesCnt: Int,
        deletedTextLength: Int,
        finalLineNumber: Int,
        finalColumn: Int
    ) {

    }

    internal fun textWidth(text: String, style: TextStyle = textStyle) = textMeasurer.map {
        it.measure(text, style = style, softWrap = false).size.width
    }.getOrElse { 0 }

    fun getLineLength(lineNumber: Int) = buffer.getLineLength(lineNumber)
    internal fun getRangeLength(range: Range) = buffer.getValueLengthInRange(range)
    fun cursorAt(offset: Int) = buffer.positionAt(offset).asCursor()
    fun offsetAt(lineNumber: Int, column: Int) = offsetAt(Cursor(lineNumber, column))
    fun offsetAt(cursor: Cursor) = buffer.offsetAt(cursor.toPosition())
    fun charAt(lineNumber: Int, column: Int) = buffer.run { getCharCode(offsetAt(lineNumber, column)).toChar() }
    fun lineCharAt(lineNumber: Int, offset: Int) = buffer.getLineCharCode(lineNumber, offset).toChar()
    fun getLine(lineNumber: Int) = buffer.getLineContent(lineNumber)
    fun lineWithLineBreak(lineNumber: Int) = buffer.getLineContentWithLineBreak(lineNumber)
    fun getLineStart(lineNumber: Int) = offsetAt(lineNumber, 1)
    fun getLineEnd(lineNumber: Int) = offsetAt(lineNumber, buffer.getLineMaxColumn(lineNumber))

    fun getLineNumberWidth(lineNumber: Int = lineCount) = textWidth(lineNumber.toString())

    fun scrollByX(dx: Float) = scrollBy(dx, 0f)
    fun scrollByY(dy: Float) = scrollBy(0f, dy)

    fun scrollBy(offset: Offset) = scrollBy(offset.x, offset.y)

    fun scrollBy(dx: Float, dy: Float) {
        val newX = maxOf(maxScrollX, (scrollState.offsetX + dx).coerceAtMost(0f))
        val newY = maxOf(maxScrollY, (scrollState.offsetY + dy).coerceAtMost(0f))
        scrollState.scrollTo(newX, newY)
    }

    fun scrollTo(x: Float, y: Float) {
        scrollState.scrollTo(
            maxOf(maxScrollX, x.coerceAtMost(0f)),
            maxOf(maxScrollY, y.coerceAtMost(0f)),
        )
    }

    fun scrollTo(offset: Offset) = scrollTo(offset.x, offset.y)

    suspend fun smoothScrollTo(x: Float, y: Float, duration: Int = 300) {
        scrollState.smoothScrollTo(
            maxOf(maxScrollX, x.coerceAtMost(0f)),
            maxOf(maxScrollY, y.coerceAtMost(0f)),
            duration
        )
    }

    suspend fun smoothScrollBy(dx: Float, dy: Float, duration: Int = 300) {
        val newX = maxOf(maxScrollX, (scrollState.offsetX + dx).coerceAtMost(0f))
        val newY = maxOf(maxScrollY, (scrollState.offsetY + dy).coerceAtMost(0f))
        scrollState.smoothScrollTo(newX, newY, duration)
    }

    suspend fun smoothScrollTo(offset: Offset, duration: Int = 300) = smoothScrollTo(offset.x, offset.y, duration)
    suspend fun smoothScrollBy(offset: Offset, duration: Int = 300) = smoothScrollBy(offset.x, offset.y, duration)

    fun getTextWidths(text: String, style: TextStyle = textStyle): FloatArray {
        val result = textMeasurer.map { it.measure(text, style = style) }.getOrThrow()
        val widths = mutableListOf<Float>()
        var prevX = 0f
        for (i in text.indices) {
            val nextX = result.getCursorRect(i + 1).left
            widths += (nextX - prevX)
            prevX = nextX
        }
        return widths.toFloatArray()
    }

    // text start indent
    fun getStartSpacing() = lineCount.count * textWidth("W") + tabWidth * 4

    // text start indent
    fun getEndSpacing() = tabWidth * 4

    internal fun measureText(
        text: String,
        style: TextStyle = textStyle,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = false,
        maxLines: Int = Int.MAX_VALUE,
        constraints: Constraints = Constraints(),
        skipCache: Boolean = false
    ) = textMeasurer.map {
        it.measure(
            text = text,
            style = style,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            constraints = constraints,
            skipCache = skipCache
        )
    }

    /**
     * Create [TextMeasurer]
     *
     * @param cacheSize Capacity of internal cache inside TextMeasurer.
     * Size unit is the number of unique text layout inputs that are
     * measured. Value of this parameter highly depends on the consumer
     * use case. Provide a cache size that is in line with how many distinct
     * text layouts are going to be calculated by this measurer repeatedly.
     * If you are animating font attributes, or any other layout affecting input,
     * cache can be skipped because most repeated measure calls would miss the cache.
     */
    internal fun <T> T.createTextMeasurer(cacheSize: Int = 8): TextMeasurer where T : DelegatableNode, T : CompositionLocalConsumerModifierNode {
        val fontFamilyResolver = currentValueOf(LocalFontFamilyResolver)
        val density = requireDensity()
        val layoutDirection = requireLayoutDirection()

        return TextMeasurer(fontFamilyResolver, density, layoutDirection, cacheSize)
    }

    internal fun invalidateDraw() = invalidateDrawCallback.invoke()

    fun copy(
        buffer: PieceTreeTextBuffer = this.buffer,
        scope: CoroutineScope = this.scope
    ) = CodeEditorState(buffer, editable, scope)
}

@Composable
fun rememberCodeEditorState(
    initialText: String = "",
    editable: Boolean = true
): CodeEditorState {
    val scope = rememberCoroutineScope { Dispatchers.Default }

    return remember(initialText, editable, scope) {
        CodeEditorState(initialText.toTextBuffer(), editable, scope)
    }
}

@Composable
fun rememberCodeEditorState(
    file: KxFile,
    editable: Boolean = true
): CodeEditorState {
    val scope = rememberCoroutineScope { Dispatchers.Default }

    val buffer by produceState(EmptyTextBuffer, key1 = file) {
        value = file.toTextBuffer()
    }

    return remember(buffer, editable, scope) {
        CodeEditorState(buffer, editable, scope)
    }
}

@Suppress("DEPRECATION")
@ExperimentalComposeCodeEditorApi
@Stable
// TODO deprecate
//@Deprecated(
//    level = DeprecationLevel.WARNING,
//    message = "It is recommended to use rememberCodeEditorState",
//    replaceWith = ReplaceWith("rememberCodeEditorState(file)", "com.klyx.editor.compose.rememberCodeEditorState")
//)
suspend fun CodeEditorState(file: KxFile): CodeEditorState {
    return CodeEditorState(
        buffer = file.toTextBuffer(),
        editable = true,
        scope = CoroutineScope(Dispatchers.Default) + DefaultMonotonicFrameClock
    )
}

@Suppress("DEPRECATION")
@ExperimentalComposeCodeEditorApi
@Stable
// TODO deprecate
//@Deprecated(
//    level = DeprecationLevel.WARNING,
//    message = "It is recommended to use rememberCodeEditorState",
//    replaceWith = ReplaceWith("rememberCodeEditorState(initialText)", "com.klyx.editor.compose.rememberCodeEditorState")
//)
fun CodeEditorState(initialText: String): CodeEditorState {
    return CodeEditorState(
        buffer = initialText.toTextBuffer(),
        editable = true,
        scope = CoroutineScope(Dispatchers.Default) + DefaultMonotonicFrameClock
    )
}
