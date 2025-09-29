package com.klyx.editor.compose.renderer

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextMeasurer
import com.klyx.editor.compose.CodeEditorState

private class EditorRendererModifierNode(
    var state: CodeEditorState,
    var onDraw: DrawScope.() -> Unit
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private val textMeasurer by lazy { createTextMeasurer() }
    private var text = state.text

    private var scrollY = 0f

    override fun ContentDrawScope.draw() {
        val result = textMeasurer.measure("")
//        val colorScheme = currentValueOf(LocalEditorColorScheme)
//        val appColorScheme = currentValueOf(LocalAppColorScheme)
//
//        clipRect {
//            drawColor(appColorScheme.background)
//
//            val textLayoutResult = textMeasurer.measure(
//                text = text.toString(),
//                style = TextStyle(
//                    color = appColorScheme.onBackground,
//                    fontSize = 18.sp,
//                ),
//                maxLines = Int.MAX_VALUE,
//                overflow = TextOverflow.Clip,
//                constraints = Constraints(
//                    maxWidth = size.width.toInt()
//                )
//            )
//
//            val textHeight = textLayoutResult.size.height
//            val viewportHeight = size.height
//
//            val maxScroll = (textHeight - viewportHeight).coerceAtLeast(0f)
//            scrollY = scrollY.coerceIn(0f, maxScroll)
//
//            translate(top = -scrollY) {
//                drawText(textLayoutResult)
//            }
//        }

        onDraw()
    }

    override fun onAttach() {
        onObservedReadsChanged()
    }

    override fun onDetach() {
        //
    }

    override fun onObservedReadsChanged() {
        observeReads {
            scrollY = state.scrollState.scrollY
        }
        invalidateDraw()
    }

    private fun createTextMeasurer(cacheSize: Int = 8): TextMeasurer {
        val fontFamilyResolver = currentValueOf(LocalFontFamilyResolver)
        val density = requireDensity()
        val layoutDirection = requireLayoutDirection()

        return TextMeasurer(fontFamilyResolver, density, layoutDirection, cacheSize)
    }
}

private data class EditorRendererModifierNodeElement(
    private val state: CodeEditorState,
    private val onDraw: DrawScope.() -> Unit
) : ModifierNodeElement<EditorRendererModifierNode>() {
    override fun InspectorInfo.inspectableProperties() {
        name = "renderEditor"
        properties["state"] = state
        properties["onDraw"] = onDraw
    }

    override fun create() = EditorRendererModifierNode(state, onDraw)

    override fun update(node: EditorRendererModifierNode) {
        node.state = state
        node.onDraw = onDraw
    }
}

internal fun Modifier.renderEditor(
    state: CodeEditorState, onDraw: DrawScope.() -> Unit = {}
) = this then EditorRendererModifierNodeElement(state, onDraw)
