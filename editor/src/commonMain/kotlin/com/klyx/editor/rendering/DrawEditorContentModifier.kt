package com.klyx.editor.rendering

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.drawText
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.editor.LocalEditorTextStyle
import com.klyx.editor.LocalTextMeasurer

@ExperimentalCodeEditorApi
private class DrawEditorContentModifier(
    var state: CodeEditorState
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode, ObserverModifierNode {
    override fun ContentDrawScope.draw() {
        val textMeasurer = currentValueOf(LocalTextMeasurer)
        val style = currentValueOf(LocalEditorTextStyle)

        drawText(
            textMeasurer = textMeasurer,
            text = "Hellow...!!!",
            style = style
        )

        drawContent()
    }

    override fun onObservedReadsChanged() {
        observeReads {

        }
    }

    override fun onAttach() {
        onObservedReadsChanged()
    }

    override fun onDetach() {

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
