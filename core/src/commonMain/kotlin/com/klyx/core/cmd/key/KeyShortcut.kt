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

@Deprecated(
    message = "Use `keyShortcutOf(key, ctrl, shift, alt, meta)` instead.",
    replaceWith = ReplaceWith("keyShortcutOf(key, ctrl, shift, alt, meta)")
)
fun keyShortcutOf(
    shortcut: String,
): KeyShortcut {
    return parseShortcut(shortcut).shortcuts.first()
}

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

@Deprecated(
    message = "Should not be used."
)
fun parseShortcut(shortcut: String): KeyShortcutSequence {
    return KeyShortcutSequence(
        shortcuts = shortcut.split(" ").map { token ->
            val parts = token.split("-")
            val key = when (val last = parts.last().lowercase()) {
                "," -> Key.Comma
                "." -> Key.Period
                ";" -> Key.Semicolon
                "esc" -> Key.Escape
                "del" -> Key.Delete
                "enter" -> Key.Enter
                "space" -> Key.Spacebar
                "up" -> Key.DirectionUp
                "down" -> Key.DirectionDown
                "left" -> Key.DirectionLeft
                "right" -> Key.DirectionRight
                "tab" -> Key.Tab
                "backspace" -> Key.Backspace
                "home" -> Key.MoveHome
                "end" -> Key.MoveEnd
                "insert" -> Key.Insert
                else -> Key.Companion::class.members.firstOrNull {
                    it.name.equals(last, true)
                }?.call(Key.Companion) as? Key ?: Key(last.first().code.toLong())
            }

            KeyShortcut(
                key = key,
                ctrl = parts.any { it.equals("Ctrl", true) },
                shift = parts.any { it.equals("Shift", true) },
                alt = parts.any { it.equals("Alt", true) },
                meta = parts.any { it.equals("Meta", true) }
            )
        }
    )
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
