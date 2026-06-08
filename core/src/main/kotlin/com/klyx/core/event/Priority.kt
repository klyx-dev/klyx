package com.klyx.core.event

/**
 * Determines the order in which subscribers receive events.
 *
 * Subscribers with higher priority values are invoked before those with lower
 * values.
 *
 * The predefined constants cover the most common cases, but custom priorities
 * can be created directly or by offsetting an existing value.
 *
 * ```kotlin
 * val critical = Priority.High + 10
 * val deferred = Priority.Normal - 5
 * ```
 */
@JvmInline
value class Priority(val value: Int) : Comparable<Priority> {

    override fun compareTo(other: Priority) = value.compareTo(other.value)

    /**
     * Returns a new priority increased by [delta].
     */
    operator fun plus(delta: Int): Priority = Priority(value + delta)

    /**
     * Returns a new priority decreased by [delta].
     */
    operator fun minus(delta: Int): Priority = Priority(value - delta)

    override fun toString(): String = when (value) {
        Int.MIN_VALUE -> "Priority.Lowest"
        25 -> "Priority.Low"
        50 -> "Priority.Normal"
        75 -> "Priority.High"
        Int.MAX_VALUE -> "Priority.Highest"
        else -> "Priority($value)"
    }

    companion object {

        /**
         * Lowest predefined priority.
         *
         * Subscribers using this priority are invoked after all others.
         */
        val Lowest = Priority(Int.MIN_VALUE)

        /**
         * Lower than the default priority.
         */
        val Low = Priority(25)

        /**
         * Default subscription priority.
         */
        val Normal = Priority(50)

        /**
         * Higher than the default priority.
         */
        val High = Priority(75)

        /**
         * Highest predefined priority.
         *
         * Subscribers using this priority are invoked before all others.
         */
        val Highest = Priority(Int.MAX_VALUE)
    }
}
