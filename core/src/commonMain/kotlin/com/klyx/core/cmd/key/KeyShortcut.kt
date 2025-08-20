package com.klyx.core.cmd.key

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import com.klyx.core.cmd.toKeyString

data class KeyShortcut(
    val ctrl: Boolean,
    val shift: Boolean,
    val alt: Boolean,
    val meta: Boolean,
    val key: Key
) {
    override fun toString(): String {
        return buildString {
            if (ctrl) append("Ctrl-")
            if (shift) append("Shift-")
            if (alt) append("Alt-")
            if (meta) append("Meta-")
            append(key.toKeyString())
        }
    }

    operator fun plus(other: KeyShortcut) = listOf(this, other)
    infix fun and(other: KeyShortcut) = plus(other)
}

fun keyShortcutOf(
    key: Key,
    ctrl: Boolean = false,
    shift: Boolean = false,
    alt: Boolean = false,
    meta: Boolean = false
): KeyShortcut = KeyShortcut(ctrl, shift, alt, meta, key)

fun Iterable<KeyShortcut>.sequence(): KeyShortcutSequence {
    return KeyShortcutSequence(shortcuts = toList())
}

data class KeyShortcutSequence(
    val shortcuts: List<KeyShortcut>,
    var currentIndex: Int = 0
) {
    fun reset() {
        currentIndex = 0
    }

    fun isComplete(): Boolean = currentIndex >= shortcuts.size

    fun advance(): Boolean {
        currentIndex++
        return isComplete()
    }
}

fun KeyEvent.matches(shortcut: KeyShortcut): Boolean {
    return key == shortcut.key &&
            (shortcut.ctrl == isCtrlPressed) &&
            (shortcut.shift == isShiftPressed) &&
            (shortcut.alt == isAltPressed) &&
            (shortcut.meta == isMetaPressed)
}

fun KeyEvent.matchesSequence(sequence: KeyShortcutSequence): Boolean {
    if (sequence.isComplete()) return false

    val currentShortcut = sequence.shortcuts[sequence.currentIndex]
    return matches(currentShortcut)
}
