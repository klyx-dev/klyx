package com.klyx.terminal.ui.selection

internal enum class HandleOrientation {
    Left,
    Right;

    fun flip(): HandleOrientation = when (this) {
        Left -> Right
        Right -> Left
    }
}
