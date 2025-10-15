package com.klyx.editor.compose.text

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import com.klyx.editor.compose.requirePrecondition
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.math.min

fun CharSequence.substring(range: Selection): String = this.substring(range.min, range.max)

/**
 * An immutable text range class, represents a text range from [start] (inclusive) to [end]
 * (exclusive). [end] can be smaller than [start] and in those cases [min] and [max] can be used in
 * order to fetch the values.
 *
 * @param start the inclusive start offset of the range. Must be non-negative, otherwise an
 *   exception will be thrown.
 * @param end the exclusive end offset of the range. Must be non-negative, otherwise an exception
 *   will be thrown.
 */
fun Selection(/*@IntRange(from = 0)*/ start: Int, /*@IntRange(from = 0)*/ end: Int) =
    Selection(packWithCheck(start, end))

/**
 * An immutable text range class, represents a text range from [start] (inclusive) to [end]
 * (exclusive). [end] can be smaller than [start] and in those cases [min] and [max] can be used in
 * order to fetch the values.
 */
@JvmInline
@Immutable
value class Selection internal constructor(private val packedValue: Long) {
    val start: Int
        get() = unpackInt1(packedValue)

    val end: Int
        get() = unpackInt2(packedValue)

    /** The minimum offset of the range. */
    val min: Int
        get() = min(start, end)

    /** The maximum offset of the range. */
    val max: Int
        get() = max(start, end)

    /** Returns true if the range is collapsed */
    val collapsed: Boolean
        get() = start == end

    /** Returns true if the start offset is larger than the end offset. */
    val reversed: Boolean
        get() = start > end

    /** Returns the length of the range. */
    val length: Int
        get() = max - min

    /** Returns true if the given range has intersection with this range */
    fun intersects(other: Selection): Boolean = (min < other.max) and (other.min < max)

    /** Returns true if this range covers including equals with the given range. */
    operator fun contains(other: Selection): Boolean = (min <= other.min) and (other.max <= max)

    /** Returns true if the given offset is a part of this range. */
    operator fun contains(offset: Int): Boolean = offset in min until max

    @Suppress("NOTHING_TO_INLINE")
    @Stable
    inline operator fun component1(): Int = start

    @Suppress("NOTHING_TO_INLINE")
    @Stable
    inline operator fun component2(): Int = end

    override fun toString(): String {
        return "Selection($start, $end)"
    }

    companion object {
        val Zero = Selection(0)
    }
}

/** Creates a [Selection] where start is equal to end, and the value of those are [index]. */
@Stable
fun Selection(index: Int): Selection = Selection(start = index, end = index)

/**
 * Ensures that [Selection.start] and [Selection.end] values lies in the specified range
 * [minimumValue] and [maximumValue]. For each [Selection.start] and [Selection.end] values:
 * - if value is smaller than [minimumValue], value is replaced by [minimumValue]
 * - if value is greater than [maximumValue], value is replaced by [maximumValue]
 *
 * @param minimumValue the minimum value that [Selection.start] or [Selection.end] can be.
 * @param maximumValue the exclusive maximum value that [Selection.start] or [Selection.end] can be.
 */
fun Selection.coerceIn(minimumValue: Int, maximumValue: Int): Selection {
    val newStart = start.fastCoerceIn(minimumValue, maximumValue)
    val newEnd = end.fastCoerceIn(minimumValue, maximumValue)
    if (newStart != start || newEnd != end) {
        return Selection(newStart, newEnd)
    }
    return this
}

fun TextRange.asSelection() = Selection(start, end)
fun Selection.asTextRange() = TextRange(start, end)

fun IntRange.toSelection() = Selection(first.coerceAtLeast(0), last.coerceAtLeast(0))

private fun packWithCheck(start: Int, end: Int): Long {
    requirePrecondition(start >= 0 && end >= 0) {
        "start and end cannot be negative. [start: $start, end: $end]"
    }
    return packInts(start, end)
}
