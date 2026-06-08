package com.klyx.terminal.emulator

import androidx.compose.ui.input.key.Key

data class TermKey(
    val key: Key,
    val mods: Int = 0
)

object KeyMod {
    const val ALT = 1 shl 0
    const val CTRL = 1 shl 1
    const val SHIFT = 1 shl 2
    const val NUM_LOCK = 1 shl 3
}
