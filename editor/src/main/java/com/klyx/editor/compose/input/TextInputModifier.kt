package com.klyx.editor.compose.input

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.establishTextInputSession
import com.klyx.editor.compose.EditorState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private class TextInputModifierNode(
    var softwareKeyboardController: SoftwareKeyboardController?,
    var editorState: EditorState,
    var sendKeyEventHandler: (KeyEvent) -> Boolean,
    var onCursorMoved: () -> Unit
) : Modifier.Node(), FocusEventModifierNode, PlatformTextInputModifierNode {
    private var focusedJob: Job? = null

    override fun onFocusEvent(focusState: FocusState) {
        focusedJob?.cancel()
        focusedJob = if (focusState.isFocused) {
            softwareKeyboardController?.show()
            coroutineScope.launch {
                establishTextInputSession {
                    val request = createInputRequest()
                    startInputMethod(request)
                }
            }
        } else {
            softwareKeyboardController?.hide()
            null
        }
    }

    private fun PlatformTextInputSession.createInputRequest(): PlatformTextInputMethodRequest {
        return object : PlatformTextInputMethodRequest {
            override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
                outAttributes.inputType = InputType.TYPE_CLASS_TEXT or 
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or 
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                outAttributes.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION or 
                    EditorInfo.IME_FLAG_NO_EXTRACT_UI
                return KlyxInputConnection.create(view, editorState, sendKeyEventHandler, onCursorMoved)
            }
        }
    }
}

private data class TextInputElement(
    private val keyboardController: SoftwareKeyboardController?,
    private val editorState: EditorState,
    private val sendKeyEventHandler: (KeyEvent) -> Boolean,
    private val onCursorMoved: () -> Unit
) : ModifierNodeElement<TextInputModifierNode>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "textInput"
        properties["keyboardController"] = keyboardController
        properties["editorState"] = editorState
        properties["sendKeyEventHandler"] = sendKeyEventHandler
        properties["onCursorMoved"] = onCursorMoved
    }

    override fun create(): TextInputModifierNode {
        return TextInputModifierNode(keyboardController, editorState, sendKeyEventHandler, onCursorMoved)
    }

    override fun update(node: TextInputModifierNode) {
        node.softwareKeyboardController = keyboardController
        node.editorState = editorState
        node.sendKeyEventHandler = sendKeyEventHandler
        node.onCursorMoved = onCursorMoved
    }
}

fun Modifier.textInput(
    keyboardController: SoftwareKeyboardController? = null,
    editorState: EditorState,
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    onCursorMoved: () -> Unit = {}
): Modifier = this then TextInputElement(keyboardController, editorState, onKeyEvent, onCursorMoved)
