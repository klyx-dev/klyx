package com.klyx.editor.compose.text

import kotlinx.serialization.Serializable

/**
 * A range in the editor. (startLine,startColumn) is <= (endLine,endColumn)
 *
 * @property startLine Line Int on which the range starts (starts at 1).
 * @property startColumn Column on which the range starts in line `startLine` (starts at 1).
 * @property endLine Line Int on which the range ends.
 * @property endColumn Column on which the range ends in line `endLine`.
 */
@Serializable
data class Range(
    var startLine: Int = 1,
    var startColumn: Int = 1,
    var endLine: Int = 1,
    var endColumn: Int = 1
) : Comparable<Range> {
    companion object {
        fun fromPositions(start: Position, end: Position = start): Range {
            return Range(start.lineNumber, start.column, end.lineNumber, end.column)
        }

        /** Returns true if the two ranges are touching in any way. */
        fun areIntersectingOrTouching(a: Range, b: Range): Boolean {
            // Check if `a` is before `b`
            if (a.endLine < b.startLine || (a.endLine == b.startLine && a.endColumn < b.startColumn)) {
                return false
            }

            // Check if `b` is before `a`
            if (b.endLine < a.startLine || (b.endLine == a.startLine && b.endColumn < a.startColumn)) {
                return false
            }

            // These ranges must intersect
            return true
        }

        /** Returns true if the two ranges are intersecting. If the ranges are touching it returns true. */
        fun areIntersecting(a: Range, b: Range): Boolean {
            // Check if `a` is before `b`
            if (a.endLine < b.startLine || (a.endLine == b.startLine && a.endColumn <= b.startColumn)) {
                return false
            }

            // Check if `b` is before `a`
            if (b.endLine < a.startLine || (b.endLine == a.startLine && b.endColumn <= a.startColumn)) {
                return false
            }

            // These ranges must intersect
            return true
        }

        /**
         * A function that compares ranges, useful for sorting ranges It will first compare ranges
         * on the startPosition and then on the endPosition
         */
        fun compareRangesUsingStarts(a: Range?, b: Range?): Int {
            if (a != null && b != null) {
                val astartLine = a.startLine
                val bstartLine = b.startLine

                if (astartLine == bstartLine) {
                    val aStartColumn = a.startColumn
                    val bStartColumn = b.startColumn

                    if (aStartColumn == bStartColumn) {
                        val aendLine = a.endLine
                        val bendLine = b.endLine

                        if (aendLine == bendLine) {
                            val aEndColumn = a.endColumn
                            val bEndColumn = b.endColumn
                            return aEndColumn - bEndColumn
                        }
                        return aendLine - bendLine
                    }
                    return aStartColumn - bStartColumn
                }
                return astartLine - bstartLine
            }
            val aExists = if (a != null) 1 else 0
            val bExists = if (b != null) 1 else 0
            return aExists - bExists
        }

        /**
         * A function that compares ranges, useful for sorting ranges It will first compare ranges
         * on the endPosition and then on the startPosition
         */
        fun compareRangesUsingEnds(a: Range, b: Range): Int {
            if (a.endLine == b.endLine) {
                if (a.endColumn == b.endColumn) {
                    if (a.startLine == b.startLine) return a.startColumn - b.startColumn

                    return a.startLine - b.startLine
                }

                return a.endColumn - b.endColumn
            }
            return a.endLine - b.endLine
        }

        /** Returns true if the range spans multiple lines. */
        fun spansMultipleLines(range: Range) = range.endLine > range.startLine
    }

    /** Return the start position (which will be before or equal to the end position) */
    val startPosition get() = Position(startLine, startColumn)

    /** Return the end position (which will be after or equal to the start position) */
    val endPosition get() = Position(endLine, endColumn)

    fun isEmpty() = startLine == endLine && startColumn == endColumn
    fun isNotEmpty() = !isEmpty()

    private fun containsPosition(position: Position): Boolean {
        if (position.lineNumber !in startLine..endLine) return false
        if (position.lineNumber == startLine && position.column < startColumn) return false
        if (position.lineNumber == endLine && position.column > endColumn) return false

        return true
    }

    private fun containsRange(range: Range): Boolean {
        if (range.startLine < startLine || range.endLine < startLine) return false
        if (range.startLine > endLine || range.endLine > endLine) return false
        if (range.startLine == startLine && range.startColumn < startColumn) return false
        if (range.endLine == endLine && range.endColumn > endColumn) return false

        return true
    }

    /** Returns true if range is in this range. If the range is equal to this range, will return true. */
    operator fun contains(range: Range) = containsRange(range)

    /** Returns true if position is in this range. If the position is at the edges, will return true. */
    operator fun contains(position: Position) = containsPosition(position)

    /**
     * Returns true if `range` is strictly in this range. `range` must start after and end before this range
     * for the result to be true.
     */
    fun strictContainsRange(range: Range): Boolean {
        if (range.startLine < startLine || range.endLine < startLine) return false
        if (range.startLine > endLine || range.endLine > endLine) return false
        if (range.startLine == startLine && range.startColumn <= startColumn) return false
        if (range.endLine == endLine && range.endColumn >= endColumn) return false

        return true
    }

    /**
     * A reunion of the two ranges. The smallest position will be used as the start point, and the
     * largest one as the end point.
     */
    operator fun plus(range: Range): Range {
        var startLine: Int
        var startColumn: Int
        var endLine: Int
        var endColumn: Int

        if (range.startLine < this.startLine) {
            startLine = range.startLine
            startColumn = range.startColumn
        } else if (range.startLine == this.startLine) {
            startLine = range.startLine
            startColumn = minOf(range.startColumn, this.startColumn)
        } else {
            startLine = this.startLine
            startColumn = this.startColumn
        }

        if (range.endLine > this.endLine) {
            endLine = range.endLine
            endColumn = range.endColumn
        } else if (range.endLine == this.endLine) {
            endLine = range.endLine
            endColumn = maxOf(range.endColumn, this.endColumn)
        } else {
            endLine = this.endLine
            endColumn = this.endColumn
        }

        return Range(startLine, startColumn, endLine, endColumn)
    }

    /** A intersection of the two ranges. */
    fun intersectRanges(range: Range): Range? {
        var resultstartLine = this.startLine
        var resultStartColumn = this.startColumn
        var resultendLine = this.endLine
        var resultEndColumn = this.endColumn

        val otherstartLine = range.startLine
        val otherStartColumn = range.startColumn
        val otherendLine = range.endLine
        val otherEndColumn = range.endColumn

        if (resultstartLine < otherstartLine) {
            resultstartLine = otherstartLine
            resultStartColumn = otherStartColumn
        } else if (resultstartLine == otherstartLine) {
            resultStartColumn = maxOf(resultStartColumn, otherStartColumn)
        }

        if (resultendLine > otherendLine) {
            resultendLine = otherendLine
            resultEndColumn = otherEndColumn
        } else if (resultendLine == otherendLine) {
            resultEndColumn = minOf(resultEndColumn, otherEndColumn)
        }

        // Check if selection is now empty
        if (resultstartLine > resultendLine) return null
        if (resultstartLine == resultendLine && resultStartColumn > resultEndColumn) return null
        return Range(resultstartLine, resultStartColumn, resultendLine, resultEndColumn)
    }

    /**
     * Create a new range using this range's start position, and using endLine and endColumn as the
     * end position.
     */
    fun setEndPosition(endLine: Int, endColumn: Int) = Range(startLine, startColumn, endLine, endColumn)

    /**
     * Create a new range using this range's end position, and using startLine and startColumn as
     * the start position.
     */
    fun setStartPosition(startLine: Int, startColumn: Int) = Range(startLine, startColumn, endLine, endColumn)

    /** Create a new empty range using this range's start position. */
    fun collapseToStart() = Range(startLine, startColumn, startLine, startColumn)

    override fun compareTo(other: Range): Int {
        if (startLine == other.startLine) {
            if (startColumn == other.startColumn) {
                if (endLine == other.endLine) return endColumn - other.endColumn
                return endLine - other.endLine
            }

            return startColumn - other.startColumn
        }
        return startLine - other.startLine
    }

    override fun toString() = "Range($startLine, $startColumn ,$endLine, $endColumn)"
}

fun Range(range: Range) = Range(range.startLine, range.startColumn, range.endLine, range.endColumn)
fun Range(start: Position, end: Position = start) = Range.fromPositions(start, end)

/**
 * @see Range.areIntersecting
 */
fun Range.areIntersecting(other: Range) = Range.areIntersecting(this, other)

/**
 * @see Range.areIntersectingOrTouching
 */
fun Range.areIntersectingOrTouching(other: Range) = Range.areIntersectingOrTouching(this, other)

/**
 * @see Range.spansMultipleLines
 */
fun Range.spansMultipleLines() = Range.spansMultipleLines(this)

/**
 * @see Range.compareRangesUsingStarts
 */
fun Range.compareUsingStarts(other: Range) = Range.compareRangesUsingStarts(this, other)

/**
 * @see Range.compareRangesUsingEnds
 */
fun Range.compareUsingEnds(other: Range) = Range.compareRangesUsingEnds(this, other)

fun Position.toRange() = Range(this)
