package com.klyx.editor.compose.renderer

import androidx.collection.LruCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import arrow.core.Some
import arrow.core.none
import com.klyx.core.LocalPlatformContext
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.EditorColorScheme
import com.klyx.editor.compose.LocalEditorColorScheme
import com.klyx.editor.compose.getOrPut
import com.klyx.editor.compose.text.Cursor
import com.klyx.editor.compose.text.Selection

internal typealias OnDraw = DrawScope.() -> Unit

internal const val CurrentLineVerticalOffset = 2f
internal const val LineDividerWidth = Stroke.HairlineWidth
internal const val LinePadding = 20f
internal const val SpacingAfterLineBackground = 4f
internal const val BUFFER_LINES = 5 // extra lines to render above/below viewport

internal val TextLineCache = LruCache<String, TextLayoutResult>(100)
internal val LineNumberCache = LruCache<String, TextLayoutResult>(100)
internal val LineWidthCache = LruCache<Int, Float>(16)

internal fun invalidateCache() {
    TextLineCache.evictAll()
    LineNumberCache.evictAll()
    LineWidthCache.evictAll()
}

private class EditorRendererModifierNode(
    var state: CodeEditorState,
    var onDraw: OnDraw,
    var showLineNumber: Boolean,
    var pinLineNumber: Boolean,
    var fontFamily: FontFamily,
    var fontSize: TextUnit,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode, ObserverModifierNode {

    private val textMeasurer by lazy { with(state) { createTextMeasurer() } }
    private val textRenderer by lazy {
        TextRenderer(context = currentValueOf(LocalPlatformContext), density = requireDensity())
    }

    // Cached line number width - only recalculate when line count digits change
    private var cachedMaxLineWidth: Float = 0f
    private var cachedLineCountDigits: Int = 0

    private val textStyle: TextStyle
        get() = state.textStyle.copy(
            fontFamily = fontFamily,
            fontSize = fontSize
        )

    override fun ContentDrawScope.draw() {
        state.viewportSize = size
        clipRect { drawEditor() }
        drawContent()
    }

    private fun updateEditorState() {
        observeReads { state._fontSize }
        observeReads { state._fontFamily }
        observeReads { fontFamily }
        observeReads { fontSize }

        state.fontFamily = fontFamily
        state.fontSize = fontSize
        state.showLineNumber = showLineNumber
        state.pinLineNumber = pinLineNumber
    }

    private val maxLineWidth: Float
        get() {
            val digits = state.lineCount.toString().length
            if (digits != cachedLineCountDigits) {
                cachedLineCountDigits = digits
                cachedMaxLineWidth = LineWidthCache.getOrPut(state.lineCount) {
                    textRenderer.measureText(state.lineCount.toString(), state.textStyle)
                }
            }
            return cachedMaxLineWidth
        }

    private fun DrawScope.drawEditor() {
        val colorScheme = currentValueOf(LocalEditorColorScheme)
        val lineHeight = state.lineHeight
        val scrollY = -state.scrollY
        val maxLine = state.lineCount
        val cursor = state.cursor
        val selection = state.selection

        val lineNumberWidth = if (showLineNumber) maxLineWidth else 0f
        val leftOffset = lineNumberWidth + LinePadding + LineDividerWidth

        val visibleStart = maxOf(0, (scrollY / lineHeight).toInt() - BUFFER_LINES)
        val visibleEnd = minOf(maxLine, ((scrollY + size.height) / lineHeight).toInt() + 1 + BUFFER_LINES)
        val visibleRange = visibleStart..visibleEnd

        if (selection.collapsed) drawCurrentLineBackground(colorScheme, lineHeight, cursor, visibleRange)
        if (!selection.collapsed) drawSelectionOptimized(
            state.selection,
            colorScheme,
            lineHeight,
            leftOffset,
            visibleRange
        )
        if (showLineNumber) drawLineNumbersBackground(lineNumberWidth + LinePadding, colorScheme)

        drawVisibleLines(visibleRange, leftOffset, lineNumberWidth, colorScheme)
    }

    private fun DrawScope.drawCurrentLineBackground(
        colorScheme: EditorColorScheme,
        lineHeight: Float,
        cursor: Cursor,
        visibleRange: IntRange
    ) {
        if (cursor.line in visibleRange && cursor.line <= state.lineCount) {
            val y = (cursor.line - 1) * lineHeight + state.scrollY + CurrentLineVerticalOffset
            drawRect(
                colorScheme.currentLineBackground,
                topLeft = Offset(0f, y),
                size = Size(size.width, lineHeight)
            )
        }
    }

    private fun DrawScope.drawLineNumbersBackground(width: Float, colorScheme: EditorColorScheme) {
        translateLineNumberIfRequired {
            drawRect(
                colorScheme.lineNumberBackground,
                topLeft = Offset.Zero,
                size = Size(width, size.height)
            )

            drawLine(
                colorScheme.lineDivider,
                start = Offset(width, 0f),
                end = Offset(width, size.height),
                strokeWidth = LineDividerWidth
            )
        }
    }

    private fun DrawScope.drawVisibleLines(
        range: IntRange,
        leftOffset: Float,
        lineNumberWidth: Float,
        colorScheme: EditorColorScheme
    ) {
        val cacheKey = "${fontSize.value}-${fontFamily.hashCode()}"

        for (lineIndex in range) {
            val lineNum = lineIndex + 1
            if (lineNum > state.lineCount) break

            val y = lineIndex * state.lineHeight + state.scrollY
            val line = state.getLine(lineNum)

            if (showLineNumber) {
                val lineResult = LineNumberCache.getOrPut("$lineNum-$cacheKey") {
                    textMeasurer.measure(
                        text = lineNum.toString(),
                        style = state.textStyle.copy(
                            textAlign = TextAlign.Right,
                            color = colorScheme.lineNumber
                        ),
                        constraints = Constraints(minWidth = (lineNumberWidth + 5).toInt())
                    )
                }

                translateLineNumberIfRequired {
                    drawText(
                        textLayoutResult = lineResult,
                        topLeft = Offset(5f, y)
                    )
                }
            }

            val lineCacheKey = "${line.hashCode()}-$cacheKey-${colorScheme.foreground.hashCode()}"
            val result = TextLineCache.getOrPut(lineCacheKey) {
                textMeasurer.measure(
                    text = line,
                    softWrap = false,
                    constraints = Constraints(maxWidth = Constraints.Infinity),
                    style = state.textStyle.copy(color = colorScheme.foreground)
                )
            }

            withTransform({
                if (pinLineNumber) clipRect(left = leftOffset)
                translate(left = leftOffset + state.scrollX + SpacingAfterLineBackground)
            }) {
                drawText(result, topLeft = Offset(0f, y))
            }
        }
    }

    private fun DrawScope.drawSelectionOptimized(
        selection: Selection,
        colorScheme: EditorColorScheme,
        lineHeight: Float,
        leftOffset: Float,
        visibleRange: IntRange
    ) {
        val (start, end) = selection.min to selection.max
        val (startLine, startCol) = state.cursorAt(start)
        val (endLine, endCol) = state.cursorAt(end)

        val renderStartLine = maxOf(startLine, visibleRange.first + 1)
        val renderEndLine = minOf(endLine, visibleRange.last)

        for (lineNum in renderStartLine..renderEndLine) {
            if (lineNum > state.lineCount) break

            val y = (lineNum - 1) * lineHeight + CurrentLineVerticalOffset
            val line = state.getLine(lineNum)

            val startIndex = if (lineNum == startLine) startCol else 0
            val endIndex = if (lineNum == endLine) endCol else line.length
            if (startIndex >= endIndex) continue

            val cacheKey =
                "${line.hashCode()}-${fontSize.value}-${fontFamily.hashCode()}-${colorScheme.selectionForeground.hashCode()}"
            val result = TextLineCache.getOrPut(cacheKey) {
                textMeasurer.measure(
                    text = line,
                    softWrap = false,
                    constraints = Constraints(maxWidth = Constraints.Infinity),
                    style = state.textStyle.copy(color = colorScheme.selectionForeground)
                )
            }

            withTransform({
                if (pinLineNumber) clipRect(left = leftOffset)
                translate(left = leftOffset + state.scrollX + SpacingAfterLineBackground, top = y + state.scrollY)
            }) {
                drawPath(
                    path = result.getPathForRange(startIndex, endIndex),
                    color = colorScheme.selectionBackground,
                    style = Fill
                )
            }
        }
    }

    private inline fun DrawScope.translateLineNumberIfRequired(block: DrawScope.() -> Unit) {
        translate(left = if (!pinLineNumber) state.scrollX else 0f, top = 0f, block = block)
    }

    override fun onAttach() {
        with(state) {
            if (textMeasurer.isNone()) textMeasurer = Some(this@EditorRendererModifierNode.textMeasurer)
            if (textRenderer.isNone()) textRenderer = Some(this@EditorRendererModifierNode.textRenderer)
            invalidateDrawCallback = this@EditorRendererModifierNode::invalidateDraw
        }
        updateEditorState()
        onObservedReadsChanged()
    }

    override fun onDetach() {
        state.textMeasurer = none()
    }

    override fun onObservedReadsChanged() {
        invalidateCache()
        invalidateDraw()
    }
}

private data class EditorRendererModifierNodeElement(
    private val state: CodeEditorState,
    private val onDraw: OnDraw,
    private val showLineNumber: Boolean,
    private val pinLineNumber: Boolean,
    private val fontFamily: FontFamily,
    private val fontSize: TextUnit
) : ModifierNodeElement<EditorRendererModifierNode>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "renderEditor"
        properties["state"] = state
        properties["onDraw"] = onDraw
        properties["showLineNumber"] = showLineNumber
        properties["pinLineNumber"] = pinLineNumber
        properties["fontFamily"] = fontFamily
        properties["fontSize"] = fontSize
    }

    override fun create() = EditorRendererModifierNode(
        state = state,
        onDraw = onDraw,
        showLineNumber = showLineNumber,
        pinLineNumber = pinLineNumber,
        fontFamily = fontFamily,
        fontSize = fontSize
    )

    override fun update(node: EditorRendererModifierNode) {
        node.state = state
        node.onDraw = onDraw
        node.showLineNumber = showLineNumber
        node.pinLineNumber = pinLineNumber
        node.fontFamily = fontFamily
        node.fontSize = fontSize
    }
}

internal fun Modifier.renderEditor(
    state: CodeEditorState,
    showLineNumber: Boolean,
    pinLineNumber: Boolean,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    onDraw: OnDraw = {}
) = this then EditorRendererModifierNodeElement(
    state = state,
    onDraw = onDraw,
    showLineNumber = showLineNumber,
    pinLineNumber = pinLineNumber,
    fontFamily = fontFamily,
    fontSize = fontSize
)
