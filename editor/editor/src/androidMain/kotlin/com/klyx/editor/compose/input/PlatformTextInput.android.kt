package com.klyx.editor.compose.input

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import com.klyx.editor.compose.CodeEditorState

internal actual suspend fun PlatformTextInputSessionScope.createInputRequest(state: CodeEditorState): PlatformTextInputMethodRequest {
    return PlatformTextInputMethodRequest { outAttributes ->
        outAttributes.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        outAttributes.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        CodeEditorInputConnection(view, state)
    }
}
