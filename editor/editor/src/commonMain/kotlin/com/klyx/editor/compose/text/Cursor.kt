package com.klyx.editor.compose.text

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import com.klyx.editor.compose.requirePrecondition
import kotlin.jvm.JvmInline

@Stable
fun Cursor(line: Int, column: Int = 0) = Cursor(packWithCheck(line, column))

/**
 * Represents the position of a cursor within a text editor.
 *
 * This value class efficiently stores the line and column number of the cursor.
 * The line number is 1-based, while the column number is 0-based.
 * For example, the very beginning of the document is at `Cursor(line = 1, column = 0)`.
 *
 * `Cursor` is an immutable data structure.
 *
 * @param line The 1-based line number of the cursor position.
 * @param column The 0-based column number (character offset) from the start of the line.
 * @property line The 1-based line number.
 * @property column The 0-based column number.
 */
@Immutable
@JvmInline
value class Cursor internal constructor(private val packedValue: Long) : Comparable<Cursor> {
    @Stable
    val line get() = unpackInt1(packedValue)

    @Stable
    val column get() = unpackInt2(packedValue)

    @Suppress("NOTHING_TO_INLINE")
    @Stable
    inline operator fun component1() = line

    @Suppress("NOTHING_TO_INLINE")
    @Stable
    inline operator fun component2() = column

    context(content: Content)
    val offset get() = content.offsetAt(this)

    override fun toString() = "Cursor(line=$line, column=$column)"

    override fun compareTo(other: Cursor): Int {
        val lineComparison = line.compareTo(other.line)
        if (lineComparison != 0) return lineComparison
        return column.compareTo(other.column)
    }

    companion object {
        @Stable
        val Start = Cursor(line = 1, column = 0)
    }
}

inline val Cursor.isAtStart: Boolean get() = line == 1 && column == 0

@Stable
internal fun Cursor(position: Position) = Cursor(position.lineNumber, position.column - 1)

@Stable
fun Cursor() = Cursor(1, 0)

@Stable
internal fun Cursor.toPosition() = Position(line, column + 1)

@Stable
internal fun Position.toCursor() = Cursor(this)

private fun packWithCheck(line: Int, column: Int): Long {
    requirePrecondition(line >= 1 && column >= 0) {
        "line and column cannot be negative. [line: $line, column: $column]"
    }
    return packInts(line, column)
}
