package com.klyx.terminal.ui.selection


internal enum class HandleOrientation {
    Left, Right
}

data class HandlePosition(
    val x: Int = 0,
    val y: Int = 0
)
