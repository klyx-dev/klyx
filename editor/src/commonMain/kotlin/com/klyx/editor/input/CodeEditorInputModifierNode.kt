package com.klyx.editor.input

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.establishTextInputSession
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@ExperimentalCodeEditorApi
private class CodeEditorInputModifierNode(
    var state: CodeEditorState,
    var editable: Boolean,
    var keyboardController: SoftwareKeyboardController?,
) : Modifier.Node(), FocusEventModifierNode, PlatformTextInputModifierNode {
    private var focusedJob: Job? = null

    override fun onFocusEvent(focusState: FocusState) {
        focusedJob?.cancel()

        if (editable) {
            focusedJob = if (focusState.isFocused) {
                keyboardController?.show()
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
    }
}

@ExperimentalCodeEditorApi
private data class CodeEditorInputElement(
    private val state: CodeEditorState,
    private val editable: Boolean,
    private val keyboardController: SoftwareKeyboardController?
) : ModifierNodeElement<CodeEditorInputModifierNode>() {
    override fun InspectorInfo.inspectableProperties() {
        name = "codeEditorInput"
        properties["state"] = state
        properties["editable"] = editable
        properties["keyboardController"] = keyboardController
    }

    override fun create() = CodeEditorInputModifierNode(state, editable, keyboardController)

    override fun update(node: CodeEditorInputModifierNode) {
        node.state = state
        node.editable = editable
        node.keyboardController = keyboardController
    }
}

@ExperimentalCodeEditorApi
internal fun Modifier.codeEditorInput(
    state: CodeEditorState,
    editable: Boolean = true,
    keyboardController: SoftwareKeyboardController? = null
) = this then CodeEditorInputElement(state, editable, keyboardController)
