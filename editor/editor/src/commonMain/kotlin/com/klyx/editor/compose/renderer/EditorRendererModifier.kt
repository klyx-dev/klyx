package com.klyx.editor.compose.renderer

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.EditorColorScheme
import com.klyx.editor.compose.LocalEditorColorScheme
import com.klyx.editor.compose.text.Cursor
import com.klyx.editor.compose.text.Selection

internal typealias OnDraw = DrawScope.() -> Unit

internal const val CurrentLineVerticalOffset = 2f
internal const val LineDividerWidth = Stroke.HairlineWidth
internal const val LinePadding = 20f
internal const val SpacingAfterLineBackground = 4f

private class EditorRendererModifierNode(
    var state: CodeEditorState,
    var onDraw: OnDraw,
    var showLineNumber: Boolean,
    var pinLineNumber: Boolean,
    var fontFamily: FontFamily,
    var fontSize: TextUnit,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode, ObserverModifierNode {

    private val textMeasurer by lazy { with(state) { createTextMeasurer() } }
    private var cursorAlpha = state.cursorAlpha

    private val textStyle: TextStyle
        get() = state.textStyle.copy(
            fontFamily = fontFamily,
            fontSize = fontSize
        )

    override fun ContentDrawScope.draw() {
        state.viewportSize = size
        clipRect { drawEditor() }
        onDraw()
    }

    private fun updateEditorState() {
        state.fontFamily = fontFamily
        state.fontSize = fontSize
        state.showLineNumber = showLineNumber
        state.pinLineNumber = pinLineNumber
    }

    private fun DrawScope.drawEditor() {
        val colorScheme = currentValueOf(LocalEditorColorScheme)
        val lineHeight = state.lineHeight
        val scrollY = state.scrollY
        val maxLine = state.lineCount
        val cursor = state.cursor
        val selection = state.selection

        val lineNumberWidth = if (showLineNumber) state.textWidth(maxLine.toString()).toFloat() else 0f
        val leftOffset = lineNumberWidth + LinePadding + LineDividerWidth

        val visibleStart = maxOf(0, (scrollY / lineHeight).toInt())
        val visibleEnd = minOf(maxLine, ((scrollY + size.height) / lineHeight).toInt() + 1)
        val visibleRange = visibleStart..visibleEnd

        if (selection.collapsed) drawCurrentLineBackground(colorScheme, lineHeight, cursor, visibleRange)
        if (!selection.collapsed) drawSelection(state.selection, colorScheme, lineHeight, leftOffset)
        if (showLineNumber) drawLineNumbersBackground(lineNumberWidth + LinePadding, colorScheme)

        drawVisibleLines(visibleRange, leftOffset, lineNumberWidth, colorScheme)
        drawCursor(cursor, leftOffset, colorScheme, lineHeight)
    }

    private fun DrawScope.drawCurrentLineBackground(
        colorScheme: EditorColorScheme,
        lineHeight: Int,
        cursor: Cursor,
        visibleRange: IntRange
    ) {
        if (cursor.line in visibleRange && cursor.line <= state.lineCount) {
            val y = (cursor.line - 1) * lineHeight - state.scrollY + CurrentLineVerticalOffset
            drawRect(
                colorScheme.currentLineBackground,
                topLeft = Offset(0f, y),
                size = Size(size.width, lineHeight.toFloat())
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
        range.forEach { lineIndex ->
            val lineNum = lineIndex + 1
            if (lineNum > state.lineCount) return@forEach

            val y = lineIndex * state.lineHeight - state.scrollY
            val line = state.getLine(lineNum)

            if (showLineNumber) {
                val lineResult = textMeasurer.measure(
                    text = lineNum.toString(),
                    style = state.textStyle.copy(
                        textAlign = TextAlign.Right,
                        color = colorScheme.lineNumber
                    ),
                    constraints = Constraints(minWidth = lineNumberWidth.toInt() + 5)
                )

                translateLineNumberIfRequired {
                    drawText(
                        textLayoutResult = lineResult,
                        topLeft = Offset(5f, y)
                    )
                }
            }

            val result = measureLineText(line, textStyle.copy(color = colorScheme.foreground))

            withTransform({
                if (pinLineNumber) clipRect(left = leftOffset)
                translate(left = leftOffset - state.scrollX + SpacingAfterLineBackground)
            }) {
                drawText(result, topLeft = Offset(0f, y))
            }
        }
    }

    private fun DrawScope.drawCursor(
        cursor: Cursor,
        leftOffset: Float,
        colorScheme: EditorColorScheme,
        lineHeight: Int
    ) {
        if (cursor.line !in 1..state.lineCount) return

        val line = state.getLine(cursor.line)
        val cursorRect = textMeasurer.measure(
            text = line,
            style = state.textStyle
        ).getCursorRect(cursor.column.coerceAtMost(line.length))

        val y = (cursor.line - 1) * lineHeight - state.scrollY + CurrentLineVerticalOffset

        withTransform({
            if (pinLineNumber) clipRect(left = leftOffset)
            translate(left = leftOffset - state.scrollX + SpacingAfterLineBackground, top = y)
        }) {
            drawRoundRect(
                color = colorScheme.cursor.copy(alpha = cursorAlpha),
                topLeft = cursorRect.topLeft,
                size = Size(2f, cursorRect.height),
                cornerRadius = CornerRadius(4f)
            )
        }
    }

    private fun DrawScope.drawSelection(
        selection: Selection,
        colorScheme: EditorColorScheme,
        lineHeight: Int,
        leftOffset: Float
    ) {
        val (start, end) = selection.min to selection.max
        val (startLine, startCol) = state.cursorAt(start)
        val (endLine, endCol) = state.cursorAt(end)

        for (lineNum in startLine..endLine) {
            if (lineNum > state.lineCount) break

            val y = (lineNum - 1) * lineHeight + CurrentLineVerticalOffset
            val line = state.getLine(lineNum)

            val startIndex = if (lineNum == startLine) startCol else 0
            val endIndex = if (lineNum == endLine) endCol else line.length
            if (startIndex >= endIndex) continue

            val result = measureLineText(line, textStyle.copy(color = colorScheme.foreground))

            withTransform({
                if (pinLineNumber) clipRect(left = leftOffset)
                translate(left = leftOffset - state.scrollX + SpacingAfterLineBackground, top = y - state.scrollY)
            }) {
                drawPath(result.getPathForRange(startIndex, endIndex), colorScheme.selectionBackground)
            }
        }
    }

    private fun measureLineText(line: String, style: TextStyle = textStyle): TextLayoutResult = textMeasurer.measure(
        text = line,
        softWrap = false,
        constraints = Constraints(maxWidth = Constraints.Infinity),
        style = style
    )

    private inline fun DrawScope.translateLineNumberIfRequired(block: DrawScope.() -> Unit) {
        translate(left = if (!pinLineNumber) -state.scrollX else 0f, top = 0f, block = block)
    }

    override fun onAttach() {
        with(state) {
            if (textMeasurer.isNone()) textMeasurer = Some(this@EditorRendererModifierNode.textMeasurer)
            invalidateDrawCallback = this@EditorRendererModifierNode::invalidateDraw
        }
        updateEditorState()

        onObservedReadsChanged()
    }

    override fun onDetach() {
        state.textMeasurer = none()
    }

    override fun onObservedReadsChanged() {
        observeReads {
            cursorAlpha = state.cursorAlpha
        }
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
