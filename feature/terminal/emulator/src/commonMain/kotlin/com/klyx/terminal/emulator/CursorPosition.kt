package com.klyx.terminal.emulator

@JvmInline
value class CursorPosition internal constructor(
    private val packedValue: Long
) {
    val row: Int
        get() = (packedValue ushr 32).toInt()

    val column: Int
        get() = packedValue.toInt()

    operator fun component1(): Int = column
    operator fun component2(): Int = row

    fun copy(row: Int = this.row, column: Int = this.column) = CursorPosition(row, column)
}

fun CursorPosition(column: Int = 0, row: Int = 0): CursorPosition {
    return CursorPosition((row.toLong() shl 32) or (column.toLong() and 0xFFFF_FFFFL))
}
