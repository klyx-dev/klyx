package com.klyx.editor.compose.event

import com.klyx.editor.compose.text.Content

sealed interface EditorEvent {
    val content: Content
}
