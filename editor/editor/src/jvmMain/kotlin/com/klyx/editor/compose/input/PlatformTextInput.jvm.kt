package com.klyx.editor.compose.input

import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import com.klyx.editor.compose.CodeEditorState

internal actual suspend fun PlatformTextInputSessionScope.createInputRequest(state: CodeEditorState): PlatformTextInputMethodRequest {
    TODO("Not yet implemented")
}
