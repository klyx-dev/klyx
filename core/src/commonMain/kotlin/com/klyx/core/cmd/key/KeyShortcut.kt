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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KeyShortcut

        if (ctrl != other.ctrl) return false
        if (shift != other.shift) return false
        if (alt != other.alt) return false
        if (meta != other.meta) return false
        if (key.keyCode != other.key.keyCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ctrl.hashCode()
        result = 31 * result + shift.hashCode()
        result = 31 * result + alt.hashCode()
        result = 31 * result + meta.hashCode()
        result = 31 * result + key.keyCode.hashCode()
        return result
    }
}

fun keyShortcutOf(
    key: Key,
    ctrl: Boolean = false,
    shift: Boolean = false,
    alt: Boolean = false,
    meta: Boolean = false
): KeyShortcut = KeyShortcut(ctrl, shift, alt, meta, key)

fun KeyEvent.asKeyShortcut() = KeyShortcut(isCtrlPressed, isShiftPressed, isAltPressed, isMetaPressed, key)

fun KeyShortcut.asSequence() = KeyShortcutSequence(shortcuts = listOf(this))

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyShortcutSequence) return false

        return currentIndex == other.currentIndex && shortcuts == other.shortcuts
    }

    override fun hashCode(): Int {
        var result = currentIndex
        result = 31 * result + shortcuts.hashCode()
        return result
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

fun KeyShortcut.matchesSequence(sequence: KeyShortcutSequence): Boolean {
    if (sequence.isComplete()) return false

    val currentShortcut = sequence.shortcuts[sequence.currentIndex]
    return this == currentShortcut
}
