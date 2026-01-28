package com.klyx.terminal.emulator

@JvmInline
value class CursorStyle private constructor(private val value: Int) {

    /**
     * The supported terminal cursor styles.
     */
    companion object {
        val Block = CursorStyle(0)
        val Underline = CursorStyle(1)
        val Bar = CursorStyle(2)

        fun availableStyles() = listOf(Block, Underline, Bar)

        @Suppress("NOTHING_TO_INLINE")
        inline fun default() = Block

        fun fromInt(value: Int) = when (value) {
            0 -> Block
            1 -> Underline
            2 -> Bar
            else -> throw IllegalArgumentException("Unknown cursor style: $value")
        }
    }
}
