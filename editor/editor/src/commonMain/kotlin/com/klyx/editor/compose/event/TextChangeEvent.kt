package com.klyx.editor.compose.event

import com.klyx.editor.compose.text.Cursor
import com.klyx.editor.compose.text.Selection

data class TextChangeEvent(
    val range: Selection,
    override val cursor: Cursor,
    val changedText: String
) : EditorEvent
