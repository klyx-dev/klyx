package com.klyx.editor.input

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import kotlinx.coroutines.currentCoroutineContext

@ExperimentalCodeEditorApi
@Suppress("ObjectLiteralToLambda")
internal actual suspend fun PlatformTextInputSessionScope.createInputRequest(state: CodeEditorState): PlatformTextInputMethodRequest {
    val coroutineContext = currentCoroutineContext()

    return object : PlatformTextInputMethodRequest {
        override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
            outAttributes.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            outAttributes.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            return CodeEditorInputConnection(view, coroutineContext, state)
        }
    }
}
