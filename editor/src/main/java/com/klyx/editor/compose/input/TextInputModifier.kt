package com.klyx.editor.compose.input

import android.text.InputType
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.establishTextInputSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private class TextInputModifierNode(
    var softwareKeyboardController: SoftwareKeyboardController?,
    var inputConnection: (View) -> InputConnection,
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
                outAttributes.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                return inputConnection(view)
            }
        }
    }
}

private data class TextInputElement(
    private val keyboardController: SoftwareKeyboardController?,
    private val inputConnection: (View) -> InputConnection,
) : ModifierNodeElement<TextInputModifierNode>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "textInput"
        properties["keyboardController"] = keyboardController
        properties["inputConnection"] = inputConnection
    }

    override fun create(): TextInputModifierNode {
        return TextInputModifierNode(keyboardController, inputConnection)
    }

    override fun update(node: TextInputModifierNode) {
        node.softwareKeyboardController = keyboardController
        node.inputConnection = inputConnection
    }
}

fun Modifier.textInput(
    keyboardController: SoftwareKeyboardController? = null,
    inputConnection: (View) -> InputConnection = { view -> BaseInputConnection(view, false) }
): Modifier = this then TextInputElement(keyboardController, inputConnection)
