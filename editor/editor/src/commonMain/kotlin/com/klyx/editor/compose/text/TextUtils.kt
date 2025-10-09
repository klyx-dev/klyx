package com.klyx.editor.compose.text

internal fun findWordBoundaries(text: String, offset: Int): IntRange {
    if (text.isEmpty()) return 0..0
    val clampedOffset = offset.coerceIn(0, text.length)

    var start = clampedOffset
    var end = clampedOffset

    while (start > 0 && text[start - 1].isWordChar()) start--
    while (end < text.length && text[end].isWordChar()) end++

    return start until end
}

private fun Char.isWordChar() = isLetterOrDigit() || this == '_'
