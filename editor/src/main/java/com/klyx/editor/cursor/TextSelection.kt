package com.klyx.editor.cursor

import androidx.compose.runtime.Immutable

@Immutable
data class TextSelection(
    val start: CursorPosition,
    val end: CursorPosition
) {
    companion object {
        val Zero = TextSelection(CursorPosition.Zero, CursorPosition.Zero)
        val Start = TextSelection(CursorPosition.Start, CursorPosition.Start)
        val Invalid = TextSelection(CursorPosition.Invalid, CursorPosition.Invalid)
    }

    val isEmpty: Boolean
        get() = start == end

    val isNotEmpty: Boolean
        get() = !isEmpty
}
