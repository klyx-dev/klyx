package com.klyx.editor.compose.event

import androidx.compose.ui.text.TextRange
import com.klyx.editor.compose.text.Cursor

data class SelectionChangeEvent(
    val selectionRange: TextRange,
    override val cursor: Cursor
) : EditorEvent

