package com.klyx.editor.input

import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi

@OptIn(ExperimentalCodeEditorApi::class)
internal expect suspend fun PlatformTextInputSessionScope.createInputRequest(
    state: CodeEditorState
): PlatformTextInputMethodRequest
