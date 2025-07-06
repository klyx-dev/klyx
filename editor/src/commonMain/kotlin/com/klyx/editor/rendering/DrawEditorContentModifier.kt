package com.klyx.editor.rendering

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.drawText
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.editor.LocalAppColorScheme
import com.klyx.editor.LocalEditorTextStyle
import com.klyx.editor.LocalTextMeasurer
import kotlin.math.ceil
import kotlin.math.floor

@ExperimentalCodeEditorApi
private class DrawEditorContentModifier(
    var state: CodeEditorState
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    override fun ContentDrawScope.draw() {
        if (size.height <= 0f) return

        val textMeasurer = currentValueOf(LocalTextMeasurer)
        val style = currentValueOf(LocalEditorTextStyle)
        val colorScheme = currentValueOf(LocalAppColorScheme)

        val lines = state.text.lines()
        val lineHeight = textMeasurer.measure("Ag", style = style).multiParagraph.getLineHeight(0)

        val visibleRange = with(state) {
            val first = maxOf(0, floor(state.scrollY / lineHeight).toInt())
            val visibleCount = minOf(lines.size - 1, ceil(size.height / lineHeight).toInt() + 2)
            val last = minOf(lines.size - 1, first + visibleCount)

            first..last
        }

        clipRect {
            drawRect(
                color = colorScheme.background
            )

            for (lineIndex in visibleRange) {
                val line = lines.getOrNull(lineIndex) ?: continue

                val y = lineIndex * lineHeight - state.scrollY
                var x = 0f

                for (char in line) {
                    val result = textMeasurer.measure(
                        text = char.toString(),
                        style = style
                    )

                    drawText(
                        result,
                        topLeft = Offset(x - state.scrollX, y)
                    )

                    x += result.size.width
                }
            }
        }
    }
}

@ExperimentalCodeEditorApi
private data class DrawEditorContentElement(
    val state: CodeEditorState
) : ModifierNodeElement<DrawEditorContentModifier>() {
    override fun create() = DrawEditorContentModifier(state)

    override fun update(node: DrawEditorContentModifier) {
        node.state = state
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawEditorContent"
        properties["state"] = state
    }
}

@ExperimentalCodeEditorApi
fun Modifier.drawEditorContent(state: CodeEditorState) = this then DrawEditorContentElement(state)
