package com.klyx.editor.compose

import androidx.compose.runtime.Stable

@Stable
fun CodeEditorState.moveCursorAtEndOfLine(select: Boolean = false) {
    val line = cursor.line
    moveCursor(line, getLineLength(line), select = select)
}

@Stable
fun CodeEditorState.moveCursorAtStartOfLine(select: Boolean = false) {
    moveCursor(cursor.line, 0, select = select)
}
