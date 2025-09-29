package com.klyx.editor.compose.text.buffer

import kotlinx.serialization.Serializable

@Serializable
data class BufferCursor(
    // Line number in current buffer line >= 0
    var line: Int,
    // Column number in current buffer column >= 0
    var column: Int
) {
    companion object {
        val Zero = BufferCursor()
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun BufferCursor() = BufferCursor(0, 0)
