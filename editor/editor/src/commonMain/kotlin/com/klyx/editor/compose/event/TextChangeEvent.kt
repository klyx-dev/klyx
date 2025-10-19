package com.klyx.editor.compose.event

import androidx.compose.ui.text.TextRange
import com.klyx.editor.compose.text.Cursor

data class TextChangeEvent(
    val range: TextRange,
    override val cursor: Cursor,
    val changedText: String
) : EditorEvent
