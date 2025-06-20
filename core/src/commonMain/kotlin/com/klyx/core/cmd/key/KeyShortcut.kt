package com.klyx.core.cmd.key

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

data class KeyShortcut(
    val key: Key,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val meta: Boolean = false
)

data class ShortcutSequence(
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

fun parseShortcut(shortcut: String): ShortcutSequence {
    return ShortcutSequence(
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
                }?.call(Key.Companion) as? Key ?: Key(last.first().code)
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

fun KeyEvent.matchesSequence(sequence: ShortcutSequence): Boolean {
    if (sequence.isComplete()) return false
    
    val currentShortcut = sequence.shortcuts[sequence.currentIndex]
    return matches(currentShortcut)
}
