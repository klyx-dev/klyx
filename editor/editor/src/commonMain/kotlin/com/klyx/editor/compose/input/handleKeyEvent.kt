package com.klyx.editor.compose.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.klyx.editor.compose.CodeEditorState

fun CodeEditorState.handleKeyEvent(event: KeyEvent) {
    if (event.type == KeyEventType.KeyDown) {
        when (event.key) {
            Key.Backspace -> {

            }
        }
    }
}
