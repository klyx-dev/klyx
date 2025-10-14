package com.klyx.editor.compose.event

import com.klyx.editor.compose.text.Cursor

sealed interface EditorEvent {
    val cursor: Cursor
}
