package com.klyx.editor.compose.event

import androidx.compose.ui.text.TextRange
import com.klyx.editor.compose.text.Content

data class TextChangeEvent(
    val range: TextRange,
    val changedText: String,
    override val content: Content
) : EditorEvent
