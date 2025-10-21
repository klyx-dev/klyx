package com.klyx.editor.compose

import androidx.compose.runtime.Stable

@Stable
fun CodeEditorState.moveCursorAtEndOfLine(select: Boolean = false) {
    val line = content.cursor.value.line
    moveCursor(line, getLineLength(line), select = select)
}

@Stable
fun CodeEditorState.moveCursorAtStartOfLine(select: Boolean = false) {
    moveCursor(content.cursor.value.line, 0, select = select)
}
