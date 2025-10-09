package com.klyx.editor.compose.text

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 *  A position in the editor.
 *
 *  @property lineNumber line number (starts at 1)
 *  @property column column (the first character in a line is between column 1 and column 2)
 */
@Serializable
@Stable
internal open class Position(var lineNumber: Int = 1, var column: Int = 1) : Comparable<Position> {
    companion object {
        /**
         * Test if position `a` is before position `b`. If the two positions are equal, the result
         * will be true.
         */
        fun isBeforeOrEqual(a: Position, b: Position): Boolean {
            if (a.lineNumber < b.lineNumber) return true
            if (b.lineNumber < a.lineNumber) return false
            return a.column <= b.column
        }

        /**
         * Test if position `a` is before position `b`. If the two positions are equal, the result
         * will be false.
         */
        fun isBefore(a: Position, b: Position): Boolean {
            if (a.lineNumber < b.lineNumber) return true
            if (b.lineNumber < a.lineNumber) return false
            return a.column < b.column
        }
    }

    /**
     * Create a new position from this position.
     *
     * @param newLineNumber new line number
     * @param newColumn new column
     */
    fun with(newLineNumber: Int = this.lineNumber, newColumn: Int = this.column): Position {
        return if (newLineNumber == this.lineNumber && newColumn == this.column) {
            this
        } else {
            Position(newLineNumber, newColumn)
        }
    }

    /**
     * Derive a new position from this position.
     *
     * @param deltaLineNumber line number delta
     * @param deltaColumn column delta
     */
    fun delta(deltaLineNumber: Int = 0, deltaColumn: Int = 0) =
        with(this.lineNumber + deltaLineNumber, this.column + deltaColumn)

    /**
     * Test if this position is before other position. If the two positions are equal, the result
     * will be false.
     */
    fun isBefore(other: Position) = isBefore(this, other)

    /**
     * Test if this position is before other position. If the two positions are equal, the result
     * will be true.
     */
    fun isBeforeOrEqual(other: Position) = isBeforeOrEqual(this, other)

    override fun compareTo(other: Position): Int {
        val result = this.lineNumber - other.lineNumber
        return if (result == 0) this.column - other.column else result
    }

    override fun toString() = "Position($lineNumber, $column)"
}

/** Create a [Position] from an `Position`. */
internal fun Position(pos: Position) = Position(pos.lineNumber, pos.column)
