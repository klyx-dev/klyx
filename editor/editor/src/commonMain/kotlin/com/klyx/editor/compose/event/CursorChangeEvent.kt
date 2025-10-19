package com.klyx.editor.compose.event

import com.klyx.editor.compose.text.Cursor

data class CursorChangeEvent(
    val oldCursor: Cursor,
    val oldCursorOffset: Int,
    val newCursor: Cursor,
    val newCursorOffset: Int,
    override val cursor: Cursor = newCursor
) : EditorEvent
