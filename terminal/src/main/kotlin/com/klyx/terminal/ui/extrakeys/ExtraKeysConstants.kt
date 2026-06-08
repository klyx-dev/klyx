package com.klyx.terminal.ui.extrakeys

import androidx.compose.ui.input.key.Key

object ExtraKeysConstants {
    val PRIMARY_REPETITIVE_KEYS = listOf("UP", "DOWN", "LEFT", "RIGHT", "BKSP", "DEL", "PGUP", "PGDN")

    typealias ExtraKeyDisplayMap = MutableMap<String, String>

    val PRIMARY_KEY_CODES_FOR_STRINGS: Map<String, Key> = mapOf(
        "SPACE" to Key.Spacebar,
        "ESC" to Key.Escape,
        "TAB" to Key.Tab,
        "HOME" to Key.MoveHome,
        "END" to Key.MoveEnd,
        "PGUP" to Key.PageUp,
        "PGDN" to Key.PageDown,
        "INS" to Key.Insert,
        "DEL" to Key.Delete,
        "BKSP" to Key.Backspace,
        "UP" to Key.DirectionUp,
        "LEFT" to Key.DirectionLeft,
        "RIGHT" to Key.DirectionRight,
        "DOWN" to Key.DirectionDown,
        "ENTER" to Key.Enter,
        "F1" to Key.F1,
        "F2" to Key.F2,
        "F3" to Key.F3,
        "F4" to Key.F4,
        "F5" to Key.F5,
        "F6" to Key.F6,
        "F7" to Key.F7,
        "F8" to Key.F8,
        "F9" to Key.F9,
        "F10" to Key.F10,
        "F11" to Key.F11,
        "F12" to Key.F12
    )

    object EXTRA_KEY_DISPLAY_MAPS {

        val CLASSIC_ARROWS_DISPLAY = mapOf(
            "LEFT" to "←",
            "RIGHT" to "→",
            "UP" to "↑",
            "DOWN" to "↓"
        )

        val WELL_KNOWN_CHARACTERS_DISPLAY = mapOf(
            "ENTER" to "↲",
            "TAB" to "↹",
            "BKSP" to "⌫",
            "DEL" to "⌦",
            "DRAWER" to "☰",
            "KEYBOARD" to "⌨",
            "PASTE" to "⎘",
            "SCROLL" to "⇳"
        )

        val LESS_KNOWN_CHARACTERS_DISPLAY = mapOf(
            "HOME" to "⇱",
            "END" to "⇲",
            "PGUP" to "⇑",
            "PGDN" to "⇓"
        )

        val ARROW_TRIANGLE_VARIATION_DISPLAY = mapOf(
            "LEFT" to "◀",
            "RIGHT" to "▶",
            "UP" to "▲",
            "DOWN" to "▼"
        )

        val NOT_KNOWN_ISO_CHARACTERS = mapOf(
            "CTRL" to "⎈",
            "ALT" to "⎇",
            "ESC" to "⎋"
        )

        val NICER_LOOKING_DISPLAY = mapOf(
            "-" to "―"
        )

        val FULL_ISO_CHAR_DISPLAY = buildMap {
            putAll(CLASSIC_ARROWS_DISPLAY)
            putAll(WELL_KNOWN_CHARACTERS_DISPLAY)
            putAll(LESS_KNOWN_CHARACTERS_DISPLAY)
            putAll(NICER_LOOKING_DISPLAY)
            putAll(NOT_KNOWN_ISO_CHARACTERS)
        }

        val ARROWS_ONLY_CHAR_DISPLAY = buildMap {
            putAll(CLASSIC_ARROWS_DISPLAY)
            putAll(NICER_LOOKING_DISPLAY)
        }

        val LOTS_OF_ARROWS_CHAR_DISPLAY = buildMap {
            putAll(CLASSIC_ARROWS_DISPLAY)
            putAll(WELL_KNOWN_CHARACTERS_DISPLAY)
            putAll(LESS_KNOWN_CHARACTERS_DISPLAY)
            putAll(NICER_LOOKING_DISPLAY)
        }

        val DEFAULT_CHAR_DISPLAY = buildMap {
            putAll(CLASSIC_ARROWS_DISPLAY)
            putAll(WELL_KNOWN_CHARACTERS_DISPLAY)
            putAll(NICER_LOOKING_DISPLAY)
        }
    }

    val CONTROL_CHARS_ALIASES = mapOf(
        "ESCAPE" to "ESC",
        "CONTROL" to "CTRL",
        "SHFT" to "SHIFT",
        "RETURN" to "ENTER",
        "FUNCTION" to "FN",

        "LT" to "LEFT",
        "RT" to "RIGHT",
        "DN" to "DOWN",

        "PAGEUP" to "PGUP",
        "PAGE_UP" to "PGUP",
        "PAGE UP" to "PGUP",
        "PAGE-UP" to "PGUP",

        "PAGEDOWN" to "PGDN",
        "PAGE_DOWN" to "PGDN",
        "PAGE-DOWN" to "PGDN",

        "DELETE" to "DEL",
        "BACKSPACE" to "BKSP",

        "BACKSLASH" to "\\",
        "QUOTE" to "\"",
        "APOSTROPHE" to "'"
    )
}
