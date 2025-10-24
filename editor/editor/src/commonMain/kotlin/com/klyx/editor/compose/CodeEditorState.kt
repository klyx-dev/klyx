package com.klyx.editor.compose

import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.substring
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import com.klyx.core.file.KxFile
import com.klyx.core.getOrThrow
import com.klyx.editor.compose.event.CursorChangeEvent
import com.klyx.editor.compose.event.EditorEvent
import com.klyx.editor.compose.event.EditorEventManager
import com.klyx.editor.compose.event.TextChangeEvent
import com.klyx.editor.compose.renderer.LineDividerWidth
import com.klyx.editor.compose.renderer.LinePadding
import com.klyx.editor.compose.renderer.LineWidthCache
import com.klyx.editor.compose.renderer.PlatformTextRenderer
import com.klyx.editor.compose.renderer.SpacingAfterLineBackground
import com.klyx.editor.compose.renderer.invalidateCache
import com.klyx.editor.compose.scroll.ScrollState
import com.klyx.editor.compose.selection.isOffsetAnEmptyLine
import com.klyx.editor.compose.text.CharUtils
import com.klyx.editor.compose.text.Content
import com.klyx.editor.compose.text.ContentChangeCallback
import com.klyx.editor.compose.text.ContentChangeList
import com.klyx.editor.compose.text.Cursor
import com.klyx.editor.compose.text.buffer.toTextBuffer
import com.klyx.editor.compose.text.emptyContent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.math.log10

@Stable
@Suppress("VariableNaming", "PropertyName")
class CodeEditorState @RememberInComposition internal constructor(
    content: Content,
    @PublishedApi
    internal val editable: Boolean,
    private val scope: CoroutineScope
) : ContentChangeCallback {

    internal val undoRedoManager = UndoRedoManager()

    var content = content
        private set(value) {
            value.state = this
            value.contentChangeCallback = this
            value.setDefaultUndoRedoManager(undoRedoManager)
            isBufferLoading = false
            field = value
        }

    internal var isBufferLoading by mutableStateOf(false)

    @Stable
    internal var _fontSize by mutableStateOf(TextUnit.Unspecified)

    @Stable
    internal var _fontFamily: FontFamily by mutableStateOf(FontFamily.Monospace)

    var fontSize: TextUnit
        get() = _fontSize
        set(value) {
            val newValue = maxOf(minOf(value.value, 40f), 7f)
            _fontSize = newValue.sp
            invalidateCache()
            lineLengthCache.clear()
            invalidateDraw()
        }

    var fontFamily
        get() = _fontFamily
        set(value) {
            _fontFamily = value
            invalidateCache()
            lineLengthCache.clear()
            invalidateDraw()
        }

    internal var colorScheme = DefaultEditorColorScheme
    internal var textMeasurer: Option<TextMeasurer> = none()
    internal var textRenderer: Option<PlatformTextRenderer> = none()

    internal val textStyle
        get() = TextStyle.Default.copy(
            fontFamily = fontFamily,
            fontSize = fontSize,
            color = colorScheme.foreground,
        )

    inline val lineCount get() = content.lineCount

    inline val cursor get() = content.cursor
    inline val selection get() = content.selection

    val cursorOffset get() = with(content) { cursor.value.offset }

    private val Int.count get() = log10(this.toDouble()).toInt() + 1

    internal val tabWidth get() = textWidth("\t")

    internal val lineHeight: Float
        get() = textRenderer.map { it.getFontMetrics(textStyle.fontSize, fontFamily).height }.getOrElse { 0f }

    internal var invalidateDrawCallback: InvalidateDrawCallback = {}
    internal var cursorAlpha by mutableStateOf(1f)
    internal var cursorRect = Rect.Zero
    internal val scrollState = ScrollState(0f, 0f)

    internal var editorLayoutCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    internal var viewportSize = Size.Zero
    internal var showLineNumber: Boolean = true
    internal var pinLineNumber: Boolean = true

    private var cachedMaxLineLength: Int = 0
    private var maxLineLengthDirty: Boolean = true
    private val lineLengthCache = mutableMapOf<Int, Int>()

    private val textWidthCache = LruCache<String, Float>(1000)

    var scrollX: Float
        get() = scrollState.offsetX
        set(value) = scrollTo(value, scrollState.offsetY)

    var scrollY: Float
        get() = scrollState.offsetY
        set(value) = scrollTo(scrollState.offsetX, value)

    internal val maxScrollX: Float
        get() {
            if (maxLineLengthDirty) {
                recalculateMaxLineLength()
            }

            val avgCharWidth = textWidth("W")
            val estimatedWidth = cachedMaxLineLength * avgCharWidth

            return (estimatedWidth + getStartSpacing() + getEndSpacing() - viewportSize.width)
                .coerceAtLeast(0f).unaryMinus()
        }

    internal val maxScrollY: Float
        get() = ((lineCount * lineHeight) - viewportSize.height / 2f).coerceAtLeast(0f).unaryMinus()

    internal val maxScrollOffset get() = Offset(maxScrollX, maxScrollY)

    val eventManager by lazy { EditorEventManager(scope) }

    internal constructor(file: KxFile, scope: CoroutineScope) : this(emptyContent(), true, scope) {
        scope.launch(Dispatchers.IO) {
            isBufferLoading = true
            content = Content(file.toTextBuffer())
            isBufferLoading = false
            maxLineLengthDirty = true
        }
    }

    private fun recalculateMaxLineLength() {
        if (lineCount == 0) {
            cachedMaxLineLength = 0
            maxLineLengthDirty = false
            return
        }

        cachedMaxLineLength = if (lineCount > 1000) {
            val samples = mutableListOf<Int>()

            // first 100 lines
            for (i in 1..minOf(100, lineCount)) {
                samples.add(getLineLengthCached(i))
            }

            // last 100 lines
            for (i in maxOf(1, lineCount - 100)..lineCount) {
                samples.add(getLineLengthCached(i))
            }

            // every 100th line in between
            var i = 100
            while (i < lineCount - 100) {
                samples.add(getLineLengthCached(i))
                i += 100
            }

            samples.maxOrNull() ?: 0
        } else {
            (1..lineCount).maxOfOrNull { getLineLengthCached(it) } ?: 0
        }

        maxLineLengthDirty = false
    }

    private fun getLineLengthCached(lineNumber: Int): Int {
        return lineLengthCache.getOrPut(lineNumber) {
            getLineLength(lineNumber)
        }
    }

    inline fun <reified E : EditorEvent> subscribeEvent(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        crossinline onEvent: suspend (E) -> Unit
    ): Job {
        return eventManager.subscribe(dispatcher, onEvent)
    }

    suspend fun <E : EditorEvent> postEvent(event: E) = eventManager.post(event)
    fun postEventSync(event: EditorEvent) = eventManager.postSync(event)

    fun setCursor(line: Int, column: Int = 0) = content.moveCursor(line, column)

    @Suppress("NOTHING_TO_INLINE")
    inline fun setCursor(cursor: Cursor) = setCursor(cursor.line, cursor.column)

    fun insert(text: CharSequence, range: TextRange = content.selection) {
        if (editable) content.insert(text, range, collapseSelection = true)
    }

    fun delete(range: TextRange = content.selection) {
        if (editable) content.delete(range)
    }

    fun setSelection(start: Int, end: Int = start) = content.setSelection(start, end)

    fun setSelection(selection: TextRange) = content.setSelection(selection)
    fun setSelection(range: IntRange) = content.setSelection(range)
    fun startOrExpandSelection(offset: Int = content.selection.start) = content.startOrExpandSelection(offset)

    @Suppress("NOTHING_TO_INLINE")
    inline fun collapseSelection() = content.collapseSelection()

    fun moveCursor(line: Int, column: Int = 0, select: Boolean = false) {
        val oldCursor = content.cursor.value
        val oldOffset = offsetAt(oldCursor)

        val l = line.coerceAtMost(lineCount)
        val c = column.coerceAtMost(getLineLength(l))
        setCursor(l, c).also {
            postEventSync(
                CursorChangeEvent(
                    oldCursor = oldCursor,
                    oldCursorOffset = oldOffset,
                    newCursor = content.cursor.value,
                    newCursorOffset = cursorOffset,
                    content = content
                )
            )
        }
        if (select) startOrExpandSelection() else collapseSelection()
        scope.launch { ensureCursorVisible() }
        invalidateDraw()
    }

    fun moveCursor(newCursor: Cursor, select: Boolean = false) {
        if (newCursor == content.cursor.value) return
        moveCursor(newCursor.line, newCursor.column, select = select)
    }

    fun moveCursorTo(offset: Int) = moveCursor(cursorAt(offset))

    fun moveCursor(direction: Direction, select: Boolean = false) {
        val (line, column) = content.cursor.value

        when (direction) {
            Direction.Left -> {
                if (column > 0) {
                    val text = getLine(line)
                    val prevEnd = column - 1
                    val prevStart = CharUtils.getCharStart(text, prevEnd)
                    setCursor(line, prevStart)
                } else if (line > 1) {
                    val prevLineLength = getLineLength(line - 1)
                    setCursor(line - 1, prevLineLength)
                }
            }

            Direction.Right -> {
                val lineText = getLine(line)
                if (column < lineText.length) {
                    val nextStart = CharUtils.getCharEnd(lineText, column)
                    setCursor(line, nextStart)
                } else if (line < lineCount) {
                    setCursor(line + 1, 0)
                }
            }

            Direction.Up -> {
                if (line > 1) {
                    val prevLineLength = getLineLength(line - 1)
                    val newColumn = column.coerceAtMost(prevLineLength)
                    setCursor(line - 1, newColumn)
                }
            }

            Direction.Down -> {
                if (line < lineCount) {
                    val nextLineLength = getLineLength(line + 1)
                    val newColumn = column.coerceAtMost(nextLineLength)
                    setCursor(line + 1, newColumn)
                }
            }
        }

        if (select) startOrExpandSelection() else collapseSelection()

        scope.launch { ensureCursorVisible() }
        invalidateDraw()
    }

    private suspend fun ensureCursorVisible(padding: Float = 48f, smooth: Boolean = true) {
        val (cursorLine, cursorColumn) = content.cursor.value

        val leftOffset = getContentLeftOffset()

        val cursorX = getTextWidthsCached(getLine(cursorLine))
            .take(maxOf(0, cursorColumn))
            .sum()

        val cursorY = (cursorLine - 1) * lineHeight

        val visibleXStart = -scrollX
        val visibleYStart = -scrollY
        val visibleXEnd = visibleXStart + viewportSize.width - leftOffset
        val visibleYEnd = visibleYStart + viewportSize.height

        var targetX = -scrollX
        var targetY = -scrollY

        if (cursorX < visibleXStart + padding) {
            targetX = (cursorX - padding).coerceAtLeast(0f)
        } else if (cursorX > visibleXEnd - padding) {
            targetX = (cursorX - (viewportSize.width - leftOffset) + padding).coerceAtLeast(0f)
        }

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
        val maxLineWidth = LineWidthCache.getOrPut(lineCount) { textWidth(lineCount.toString()) }
        val lineNumberWidth = if (showLineNumber) maxLineWidth else 0f
        return lineNumberWidth + LinePadding + LineDividerWidth + SpacingAfterLineBackground
    }

    internal fun calculateCursorPositionFromScreenOffset(offset: Offset): Cursor {
        val lineNumber = ((offset.y - scrollState.offsetY) / lineHeight).toInt() + 1
        val clampedLine = lineNumber.coerceIn(1, lineCount)
        val leftOffset = getContentLeftOffset()

        val adjustedX = (offset.x - leftOffset - scrollState.offsetX).coerceAtLeast(0f)

        val line = getLine(clampedLine)
        if (line.isEmpty()) return Cursor(clampedLine, 0)

        val widths = getTextWidthsCached(line)
        var currentWidth = 0f
        var column = 0

        for (i in widths.indices) {
            val charWidth = widths[i]
            if (adjustedX < currentWidth + charWidth / 2) {
                break
            }
            currentWidth += charWidth
            column++
        }

        return Cursor(clampedLine, column)
    }

    internal fun textWidth(text: String, style: TextStyle = textStyle): Float {
        val cacheKey = "$text-${style.fontSize.value}-${style.fontFamily.hashCode()}"
        return textWidthCache.getOrPut(cacheKey) {
            textRenderer.map { it.measureText(text, style) }.getOrElse { 0f }
        }
    }

    internal fun getTextDirectionForOffset(offset: Int): ResolvedTextDirection {
        val (line, column) = cursorAt(offset)
        val result = measureText(getLine(line))
            .getOrElse { return ResolvedTextDirection.Ltr }
        return with(result) {
            if (isOffsetAnEmptyLine(column)) getParagraphDirection(column) else getBidiRunDirection(column)
        }
    }

    internal fun getBidiRunDirection(offset: Int): ResolvedTextDirection {
        val (line, column) = cursorAt(offset)
        val result = measureText(getLine(line))
            .getOrElse { return ResolvedTextDirection.Ltr }
        return with(result) { getBidiRunDirection(column) }
    }

    internal fun getCursorRect(offset: Int): Rect {
        val (line, column) = cursorAt(offset)
        val result = measureText(getLine(line)).getOrElse { return Rect.Zero }
        return with(result) { getCursorRect(column) }
    }

    internal fun getLineStart(line: Int): Int {
        return offsetAt(line, 0)
    }

    internal fun getLineEnd(line: Int): Int {
        return offsetAt(line, content.maxColumnForLine(line))
    }

    internal fun getWordBoundary(offset: Int): TextRange {
        val (line, column) = cursorAt(offset)
        val result = measureText(getLine(line)).getOrElse { return TextRange.Zero }
        return with(result) { getWordBoundary(column) }
    }

    fun getLineLength(lineNumber: Int) = content.lengthOf(line = lineNumber)
    fun getLine(lineNumber: Int) = content.lineText(lineNumber)
    fun cursorAt(offset: Int) = content.cursorAt(offset)
    fun offsetAt(cursor: Cursor) = content.offsetAt(cursor)
    fun offsetAt(line: Int, column: Int) = content.offsetAt(line, column)

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

    private val textWidthsCache = LruCache<String, FloatArray>(500)

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

    private fun getTextWidthsCached(text: String, style: TextStyle = textStyle): FloatArray {
        val cacheKey = "${text.hashCode()}-${style.fontSize.value}-${style.fontFamily.hashCode()}"
        return textWidthsCache.getOrPut(cacheKey) {
            getTextWidths(text, style)
        }
    }

    fun getStartSpacing() = lineCount.count * textWidth("W") + tabWidth * 4

    fun getEndSpacing() = tabWidth * 4

    internal fun measureText(
        text: String,
        style: TextStyle = textStyle,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = false,
        maxLines: Int = Int.MAX_VALUE,
        constraints: Constraints = Constraints(maxWidth = Constraints.Infinity),
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

    internal fun <T> T.createTextMeasurer(cacheSize: Int = 8): TextMeasurer where T : DelegatableNode, T : CompositionLocalConsumerModifierNode {
        val fontFamilyResolver = currentValueOf(LocalFontFamilyResolver)
        val density = requireDensity()
        val layoutDirection = requireLayoutDirection()

        return TextMeasurer(fontFamilyResolver, density, layoutDirection, cacheSize)
    }

    internal fun invalidateDraw() {
        invalidateDrawCallback.invoke()
    }

    fun copy(
        content: Content = this.content,
        scope: CoroutineScope = this.scope
    ) = CodeEditorState(content, editable, scope)

    override fun onContentChanged(
        range: TextRange,
        rangeOffset: Int,
        insertedLinesCount: Int,
        insertedTextLength: Int,
        deletedLinesCount: Int,
        deletedTextLength: Int,
        finalLineNumber: Int,
        finalColumn: Int
    ) {
        eventManager.postSync(
            event = TextChangeEvent(
                range = range,
                changedText = content.substring(range),
                content = content
            )
        )
    }

    override fun afterContentChanged(
        changeList: ContentChangeList,
        lastLineNumber: Int,
        lastColumn: Int
    ) {
        maxLineLengthDirty = true
        lineLengthCache.clear()
        textWidthCache.evictAll()

        setCursor(lastLineNumber, lastColumn - 1)
        scope.launch { ensureCursorVisible(smooth = false) }

        invalidateDraw()
    }
}

@Composable
fun rememberCodeEditorState(
    initialText: String = "",
    editable: Boolean = true
): CodeEditorState {
    val scope = rememberCoroutineScope { Dispatchers.Default }

    return remember(initialText, editable, scope) {
        CodeEditorState(Content(initialText), editable, scope)
    }
}

@Composable
fun rememberCodeEditorState(file: KxFile): CodeEditorState {
    val scope = rememberCoroutineScope { Dispatchers.Default }
    return remember(file) { CodeEditorState(file, scope) }
}

@Suppress("DEPRECATION")
@ExperimentalComposeCodeEditorApi
@Stable
@RememberInComposition
fun CodeEditorState(file: KxFile): CodeEditorState {
    return CodeEditorState(
        file = file,
        scope = CoroutineScope(Dispatchers.Default) + DefaultMonotonicFrameClock
    )
}

@Suppress("DEPRECATION")
@ExperimentalComposeCodeEditorApi
@Stable
@RememberInComposition
fun CodeEditorState(initialText: String): CodeEditorState {
    return CodeEditorState(
        content = Content(initialText),
        editable = true,
        scope = CoroutineScope(Dispatchers.Default) + DefaultMonotonicFrameClock
    )
}
