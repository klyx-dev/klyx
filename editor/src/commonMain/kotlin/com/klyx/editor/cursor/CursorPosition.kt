package com.klyx.editor.cursor

import androidx.compose.runtime.Immutable
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.lineSeparator

@Immutable
@ExperimentalCodeEditorApi
data class CursorPosition(
    val offset: Int = 0
) : Comparable<CursorPosition> {

    companion object {
        val Initial = CursorPosition(0)
    }

    override fun compareTo(other: CursorPosition): Int {
        return offset.compareTo(other.offset)
    }
}

@ExperimentalCodeEditorApi
internal fun Pair<Int, Int>.toCursorOffset(sb: StringBuilder): Int = run {
    val (line, column) = this
    val lines = sb.split(lineSeparator)

    if (line !in lines.indices) return -1
    if (column !in 0..lines[line].length) return -1

    val offsetBeforeLine = lines.take(line).sumOf { it.length + lineSeparator.length }
    return offsetBeforeLine + column
}

internal fun offsetToLineColumn(sb: StringBuilder, offset: Int): Pair<Int, Int> {
    require(offset in 0..sb.length) { "Offset out of bounds" }

    val lineSeparatorLength = lineSeparator.length

    var line = 0
    var column = 0
    var i = 0

    while (i < offset) {
        if (i + lineSeparatorLength <= sb.length &&
            sb.substring(i, i + lineSeparatorLength) == lineSeparator) {
            line++
            column = 0
            i += lineSeparatorLength
        } else {
            column++
            i++
        }
    }

    return line to column
}

internal const val CURSOR_BLINK_RATE = 500
