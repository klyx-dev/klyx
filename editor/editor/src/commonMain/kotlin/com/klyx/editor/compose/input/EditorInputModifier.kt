package com.klyx.editor.compose.input

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.establishTextInputSession
import com.klyx.editor.compose.CodeEditorState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private class EditorInputModifierNode(
    var state: CodeEditorState,
    var editable: Boolean
) : Modifier.Node(), FocusEventModifierNode, PlatformTextInputModifierNode, CompositionLocalConsumerModifierNode,
    ObserverModifierNode {

    private var focusedJob: Job? = null
    private var keyboardController: SoftwareKeyboardController? = null

    override fun onFocusEvent(focusState: FocusState) {
        focusedJob?.cancel()
        if (!editable) return

        focusedJob = if (focusState.isFocused) {
            coroutineScope.launch {
                establishTextInputSession {
                    startInputMethod(createInputRequest(state))
                }
            }
        } else {
            keyboardController?.hide()
            null
        }
    }

    override fun onAttach() {
        onObservedReadsChanged()
    }

    override fun onDetach() {
        keyboardController = null
    }

    override fun onObservedReadsChanged() {
        observeReads {
            keyboardController = currentValueOf(LocalSoftwareKeyboardController)
        }
    }
}

private data class EditorInputModifierNodeElement(
    private val state: CodeEditorState,
    private val editable: Boolean
) : ModifierNodeElement<EditorInputModifierNode>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "editorInput"
        properties["state"] = state
        properties["editable"] = editable
    }

    override fun create() = EditorInputModifierNode(state, editable)

    override fun update(node: EditorInputModifierNode) {
        node.state = state
        node.editable = editable
    }
}

internal fun Modifier.editorInput(
    state: CodeEditorState,
    editable: Boolean = true
) = this then EditorInputModifierNodeElement(state, editable)
