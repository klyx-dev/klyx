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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.TextLayoutResult
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
import kotlinx.coroutines.Job

internal const val CurrentLineVerticalOffset = 2f
internal const val LineDividerWidth = Stroke.HairlineWidth
internal const val LinePadding = 20f
internal const val SpacingAfterLineBackground = 4f
internal const val BUFFER_LINES = 5 // extra lines to render above/below viewport

// Keep caches keyed by String to maintain compatibility with other modules using them
internal val TextLineCache = LruCache<String, TextLayoutResult>(160)
internal val LineNumberCache = LruCache<String, TextLayoutResult>(160)
internal val LineWidthCache = LruCache<Int, Float>(16)

internal fun invalidateCache() {
    TextLineCache.evictAll()
    LineNumberCache.evictAll()
    LineWidthCache.evictAll()
}

private class EditorRendererModifierNode(
    private var state: CodeEditorState,
    private var showLineNumber: Boolean,
    private var pinLineNumber: Boolean,
    private var fontFamily: FontFamily,
    private var fontSize: TextUnit
) : DelegatingNode(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode,
    GlobalPositionAwareModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    private val textMeasurer by lazy { with(state) { createTextMeasurer() } }
    private val textRenderer by lazy {
        PlatformTextRenderer(context = currentValueOf(LocalPlatformContext), density = requireDensity())
    }

    private var cachedMaxLineWidth: Float = 0f
    private var cachedLineCountDigits: Int = 0
    private var cachedColorScheme: EditorColorScheme? = null
    private var cachePrefix: String = ""

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

    private var toolbarAndHandlesVisibilityObserverJob: Job? = null

    override fun ContentDrawScope.draw() {
        if (!isAttached) return

        state.viewportSize = size
        clipRect { drawEditor() }
        drawContent()
    }

    fun update(
        state: CodeEditorState,
        showLineNumber: Boolean,
        pinLineNumber: Boolean,
        fontFamily: FontFamily,
        fontSize: TextUnit
    ) {
        var changed = false
        var fontChanged = false

        if (this.state != state) {
            this.state = state
            changed = true
        }

        if (this.showLineNumber != showLineNumber) {
            this.showLineNumber = showLineNumber
            changed = true
        }

        if (this.pinLineNumber != pinLineNumber) {
            this.pinLineNumber = pinLineNumber
            changed = true
        }

        if (this.fontFamily != fontFamily) {
            this.fontFamily = fontFamily
            changed = true
            fontChanged = true
        }

        if (this.fontSize != fontSize) {
            this.fontSize = fontSize
            changed = true
            fontChanged = true
        }

        if (fontChanged) {
            cachedLineCountDigits = 0
            cachedMaxLineWidth = 0f
            cachePrefix = buildString(32) {
                append(fontSize.value)
                append('-')
                append(fontFamily.hashCode())
            }
        }

        if (changed && isAttached) {
            updateEditorState()
            invalidateDraw()
        }
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

    private fun DrawScope.drawEditor() {
        val colorScheme = currentValueOf(LocalEditorColorScheme)

        if (cachedColorScheme != colorScheme) {
            cachedColorScheme = colorScheme
            TextLineCache.evictAll()
        }

        val lineHeight = state.lineHeight
        val scrollY = -state.scrollY
        val maxLine = state.lineCount
        val cursor = state.content.cursor.value
        // Selection rendering removed for performance and simplification

        val lineNumberWidth = if (showLineNumber) maxLineWidth else 0f
        val leftOffset = lineNumberWidth + LinePadding + LineDividerWidth

        val visibleStart = maxOf(0, (scrollY / lineHeight).toInt() - BUFFER_LINES)
        val visibleEnd = minOf(maxLine, ((scrollY + size.height) / lineHeight).toInt() + 1 + BUFFER_LINES)
        val visibleRange = visibleStart..visibleEnd

        // Always draw current line background
        drawCurrentLineBackground(colorScheme, lineHeight, cursor, visibleRange)

        if (showLineNumber) {
            drawLineNumbersBackground(lineNumberWidth + LinePadding, colorScheme)
        }

        drawTextLines(visibleRange, leftOffset, lineNumberWidth, colorScheme)
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

    private fun DrawScope.drawTextLines(
        range: IntRange,
        leftOffset: Float,
        lineNumberWidth: Float,
        colorScheme: EditorColorScheme
    ) {
        val cachePrefix = this@EditorRendererModifierNode.cachePrefix

        // Draw line numbers without applying the text column transform
        if (showLineNumber) {
            for (lineIndex in range) {
                val lineNum = lineIndex + 1
                if (lineNum > state.lineCount) break
                val y = lineIndex * state.lineHeight + state.scrollY
                val numberKey = "ln-$lineNum-$cachePrefix-${colorScheme.lineNumber.hashCode()}-${(lineNumberWidth + 5).toInt()}"
                val minWidth = (lineNumberWidth + 5).toInt()
                val lineResult = LineNumberCache.getOrPut(numberKey) {
                    textMeasurer.measure(
                        text = lineNum.toString(),
                        style = state.textStyle.copy(
                            textAlign = TextAlign.Right,
                            color = colorScheme.lineNumber
                        ),
                        constraints = Constraints(minWidth = minWidth)
                    )
                }
                translateLineNumberIfRequired {
                    drawText(
                        textLayoutResult = lineResult,
                        topLeft = Offset(5f, y)
                    )
                }
            }
        }

        // Pre-apply transforms once for the text column to reduce per-line state changes
        val baseLeft = leftOffset + state.scrollX + SpacingAfterLineBackground
        withTransform({
            if (pinLineNumber) clipRect(left = leftOffset)
            translate(left = baseLeft)
        }) {
            for (lineIndex in range) {
                val lineNum = lineIndex + 1
                if (lineNum > state.lineCount) break
                val y = lineIndex * state.lineHeight + state.scrollY
                val line = state.getLine(lineNum)
                val lineKey = "tx-${line.hashCode()}-$cachePrefix-${colorScheme.foreground.hashCode()}"
                val result = TextLineCache.getOrPut(lineKey) {
                    textMeasurer.measure(
                        text = line,
                        softWrap = false,
                        constraints = Constraints(maxWidth = Constraints.Infinity),
                        style = state.textStyle.copy(color = colorScheme.foreground)
                    )
                }
                drawText(result, topLeft = Offset(0f, y))
            }
        }
    }

    // Selection drawing removed

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
        state.textRenderer = none()
        state.invalidateDrawCallback = {}
    }

    override fun onObservedReadsChanged() {
        if (!isAttached) return
        // Avoid global cache eviction on every observed read; invalidate draw only.
        invalidateDraw()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        state.editorLayoutCoordinates = coordinates
    }
}

private data class EditorRendererModifierNodeElement(
    private val state: CodeEditorState,
    private val showLineNumber: Boolean,
    private val pinLineNumber: Boolean,
    private val fontFamily: FontFamily,
    private val fontSize: TextUnit
) : ModifierNodeElement<EditorRendererModifierNode>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "renderEditor"
        properties["state"] = state
        properties["showLineNumber"] = showLineNumber
        properties["pinLineNumber"] = pinLineNumber
        properties["fontFamily"] = fontFamily
        properties["fontSize"] = fontSize
    }

    override fun create() = EditorRendererModifierNode(
        state = state,
        showLineNumber = showLineNumber,
        pinLineNumber = pinLineNumber,
        fontFamily = fontFamily,
        fontSize = fontSize
    )

    override fun update(node: EditorRendererModifierNode) {
        node.update(
            state = state,
            showLineNumber = showLineNumber,
            pinLineNumber = pinLineNumber,
            fontFamily = fontFamily,
            fontSize = fontSize
        )
    }
}

internal fun Modifier.renderEditor(
    state: CodeEditorState,
    showLineNumber: Boolean,
    pinLineNumber: Boolean,
    fontFamily: FontFamily,
    fontSize: TextUnit
) = this then EditorRendererModifierNodeElement(
    state = state,
    showLineNumber = showLineNumber,
    pinLineNumber = pinLineNumber,
    fontFamily = fontFamily,
    fontSize = fontSize
)
