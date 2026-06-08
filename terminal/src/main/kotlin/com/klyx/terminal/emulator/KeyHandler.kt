package com.klyx.terminal.emulator

import androidx.compose.ui.input.key.Key

object KeyHandler {
    // terminfo: http://pubs.opengroup.org/onlinepubs/7990989799/xcurses/terminfo.html
    // termcap: http://man7.org/linux/man-pages/man5/termcap.5.html
    private val TERMCAP_TO_KEY = mapOf(
        "%i" to TermKey(Key.DirectionRight, KeyMod.SHIFT),
        "#2" to TermKey(Key.MoveHome, KeyMod.SHIFT), // Shifted home
        "#4" to TermKey(Key.DirectionLeft, KeyMod.SHIFT),
        "*7" to TermKey(Key.MoveEnd, KeyMod.SHIFT), // Shifted end key

        "k1" to TermKey(Key.F1),
        "k2" to TermKey(Key.F2),
        "k3" to TermKey(Key.F3),
        "k4" to TermKey(Key.F4),
        "k5" to TermKey(Key.F5),
        "k6" to TermKey(Key.F6),
        "k7" to TermKey(Key.F7),
        "k8" to TermKey(Key.F8),
        "k9" to TermKey(Key.F9),
        "k;" to TermKey(Key.F10),
        "F1" to TermKey(Key.F11),
        "F2" to TermKey(Key.F12),
        "F3" to TermKey(Key.F1, KeyMod.SHIFT),
        "F4" to TermKey(Key.F2, KeyMod.SHIFT),
        "F5" to TermKey(Key.F3, KeyMod.SHIFT),
        "F6" to TermKey(Key.F4, KeyMod.SHIFT),
        "F7" to TermKey(Key.F5, KeyMod.SHIFT),
        "F8" to TermKey(Key.F6, KeyMod.SHIFT),
        "F9" to TermKey(Key.F7, KeyMod.SHIFT),
        "FA" to TermKey(Key.F8, KeyMod.SHIFT),
        "FB" to TermKey(Key.F9, KeyMod.SHIFT),
        "FC" to TermKey(Key.F10, KeyMod.SHIFT),
        "FD" to TermKey(Key.F11, KeyMod.SHIFT),
        "FE" to TermKey(Key.F12, KeyMod.SHIFT),

        "kb" to TermKey(Key.Backspace), // backspace key

        "kd" to TermKey(Key.DirectionDown), // terminfo=kcud1, down-arrow key
        "kh" to TermKey(Key.MoveHome),
        "kl" to TermKey(Key.DirectionLeft),
        "kr" to TermKey(Key.DirectionRight),

        // K1=Upper left of keypad:
        // t_K1 <kHome> keypad home key
        // t_K3 <kPageUp> keypad page-up key
        // t_K4 <kEnd> keypad end key
        // t_K5 <kPageDown> keypad page-down key
        "K1" to TermKey(Key.MoveHome),
        "K3" to TermKey(Key.PageUp),
        "K4" to TermKey(Key.MoveEnd),
        "K5" to TermKey(Key.PageDown),

        "ku" to TermKey(Key.DirectionUp),

        "kB" to TermKey(Key.Tab, KeyMod.SHIFT), // termcap=kB, terminfo=kcbt: Back-tab
        "kD" to TermKey(Key.Delete), // terminfo=kdch1, delete-character key
        "kDN" to TermKey(Key.DirectionDown, KeyMod.SHIFT), // non-standard shifted arrow down
        "kF" to TermKey(Key.DirectionDown, KeyMod.SHIFT), // terminfo=kind, scroll-forward key
        "kI" to TermKey(Key.Insert),
        "kN" to TermKey(Key.PageUp),
        "kP" to TermKey(Key.PageDown),
        "kR" to TermKey(Key.DirectionUp, KeyMod.SHIFT), // terminfo=kri, scroll-backward key
        "kUP" to TermKey(Key.DirectionUp, KeyMod.SHIFT), // non-standard shifted up

        "@7" to TermKey(Key.MoveEnd),
        "@8" to TermKey(Key.NumPadEnter)
    )

    fun getCodeFromTermcap(termcap: String, cursorKeysApp: Boolean, keypadApp: Boolean): String? {
        val key = TERMCAP_TO_KEY[termcap] ?: return null
        return getCode(key, cursorKeysApp, keypadApp)
    }

    fun getCode(termKey: TermKey, cursorApp: Boolean, keypadApp: Boolean): String? {
        val key = termKey.key
        val keyMod = termKey.mods
        val numLock = keyMod and KeyMod.NUM_LOCK != 0

        fun mod(start: String, end: Char) = transformForModifiers(start, keyMod, end)

        return when (key) {
            Key.DirectionCenter -> "\u000D"

            Key.DirectionUp -> if (keyMod == 0) if (cursorApp) "\u001BOA" else "\u001B[A" else mod("\u001B[1", 'A')
            Key.DirectionDown -> if (keyMod == 0) if (cursorApp) "\u001BOB" else "\u001B[B" else mod("\u001B[1", 'B')
            Key.DirectionRight -> if (keyMod == 0) if (cursorApp) "\u001BOC" else "\u001B[C" else mod("\u001B[1", 'C')
            Key.DirectionLeft -> if (keyMod == 0) if (cursorApp) "\u001BOD" else "\u001B[D" else mod("\u001B[1", 'D')

            Key.MoveHome -> {
                // Note that KEYCODE_HOME is handled by the system and never delivered to applications.
                // On a Logitech k810 keyboard KEYCODE_MOVE_HOME is sent by FN+LeftArrow.
                if (keyMod == 0) if (cursorApp) "\u001BOH" else "\u001B[H" else mod("\u001B[1", 'H')
            }

            Key.MoveEnd -> if (keyMod == 0) if (cursorApp) "\u001BOF" else "\u001B[F" else mod("\u001B[1", 'F')

            // An xterm can send function keys F1 to F4 in two modes: vt100 compatible or
            // not. Because Vim may not know what the xterm is sending, both types of keys
            // are recognized. The same happens for the <Home> and <End> keys.
            // normal vt100 ~
            // <F1> t_k1 <Esc>[11~ <xF1> <Esc>OP *<xF1>-xterm*
            // <F2> t_k2 <Esc>[12~ <xF2> <Esc>OQ *<xF2>-xterm*
            // <F3> t_k3 <Esc>[13~ <xF3> <Esc>OR *<xF3>-xterm*
            // <F4> t_k4 <Esc>[14~ <xF4> <Esc>OS *<xF4>-xterm*
            // <Home> t_kh <Esc>[7~ <xHome> <Esc>OH *<xHome>-xterm*
            // <End> t_@7 <Esc>[4~ <xEnd> <Esc>OF *<xEnd>-xterm*
            Key.F1 -> if (keyMod == 0) "\u001BOP" else mod("\u001B[1", 'P')
            Key.F2 -> if (keyMod == 0) "\u001BOQ" else mod("\u001B[1", 'Q')
            Key.F3 -> if (keyMod == 0) "\u001BOR" else mod("\u001B[1", 'R')
            Key.F4 -> if (keyMod == 0) "\u001BOS" else mod("\u001B[1", 'S')
            Key.F5 -> mod("\u001B[15", '~')
            Key.F6 -> mod("\u001B[17", '~')
            Key.F7 -> mod("\u001B[18", '~')
            Key.F8 -> mod("\u001B[19", '~')
            Key.F9 -> mod("\u001B[20", '~')
            Key.F10 -> mod("\u001B[21", '~')
            Key.F11 -> mod("\u001B[23", '~')
            Key.F12 -> mod("\u001B[24", '~')

            Key.PrintScreen -> "\u001B[32~" // Sys Request / Print
            // Is this Scroll lock? case Cancel: return "\033[33~";
            Key.Break -> "\u001B[34~" // Pause/Break

            Key.Escape, Key.Back -> "\u001B"

            Key.Insert -> mod("\u001B[2", '~')
            Key.Delete -> mod("\u001B[3", '~')
            Key.PageUp -> mod("\u001B[5", '~')
            Key.PageDown -> mod("\u001B[6", '~')

            Key.Backspace -> {
                val prefix = if (keyMod and KeyMod.ALT != 0) "\u001B" else ""
                // Just do what xterm and gnome-terminal does:
                prefix + if (keyMod and KeyMod.CTRL != 0) "\u0008" else "\u007F"
            }

            Key.NumLock -> if (keypadApp) "\u001BOP" else null
            Key.Spacebar -> {
                // If ctrl is not down, return null so that it goes through normal input processing (which may e.g. cause a
                // combining accent to be written):
                if (keyMod and KeyMod.CTRL == 0) null else "\u0000"
            }

            Key.Tab -> {
                // This is back-tab when shifted:
                if (keyMod and KeyMod.SHIFT == 0) "\u0009" else "\u001B[Z"
            }

            Key.Enter -> if (keyMod and KeyMod.ALT != 0) "\u001B\r" else "\r"
            Key.NumPadEnter -> if (keypadApp) mod("\u001BO", 'M') else "\n"
            Key.NumPadMultiply -> if (keypadApp) mod("\u001BO", 'j') else "*"
            Key.NumPadAdd -> if (keypadApp) mod("\u001BO", 'k') else "+"
            Key.NumPadComma -> ","
            Key.NumPadDot -> {
                if (numLock) {
                    if (keypadApp) "\u001BOn" else "."
                } else {
                    // DELETE
                    mod("\u001B[3", '~')
                }
            }

            Key.NumPadSubtract -> if (keypadApp) mod("\u001BO", 'm') else "-"
            Key.NumPadDivide -> if (keypadApp) mod("\u001BO", 'o') else "/"

            Key.NumPad0 -> {
                if (numLock) {
                    if (keypadApp) mod("\u001BO", 'p') else "0"
                } else {
                    // INSERT
                    mod("\u001B[2", '~')
                }
            }

            Key.NumPad1 -> {
                if (numLock) {
                    if (keypadApp) mod("\u001BO", 'q') else "1"
                } else {
                    // END
                    if (keyMod == 0) {
                        if (cursorApp) "\u001BOF" else "\u001B[F"
                    } else {
                        mod("\u001B[1", 'F')
                    }
                }
            }

            Key.NumPad2 -> {
                if (numLock) {
                    if (keypadApp) mod("\u001BO", 'r') else "2"
                } else {
                    // DOWN
                    if (keyMod == 0) {
                        if (cursorApp) "\u001BOB" else "\u001B[B"
                    } else {
                        mod("\u001B[1", 'B')
                    }
                }
            }

            Key.NumPad3 -> {
                if (numLock) {
                    if (keypadApp) mod("\u001BO", 's') else "3"
                } else {
                    // PGDN
                    "\u001B[6~"
                }
            }

            Key.NumPad4 -> {
                if (numLock) {
                    if (keypadApp) mod("\u001BO", 't') else "4"
                } else {
                    // LEFT
                    if (keyMod == 0) {
                        if (cursorApp) "\u001BOD" else "\u001B[D"
                    } else {
                        mod("\u001B[1", 'D')
                    }
                }
            }

            Key.NumPad5 -> if (keypadApp) mod("\u001BO", 'u') else "5"

            Key.NumPad6 -> {
                if (numLock) {
                    if (keypadApp) mod("\u001BO", 'v') else "6"
                } else {
                    // RIGHT
                    if (keyMod == 0) {
                        if (cursorApp) "\u001BOC" else "\u001B[C"
                    } else {
                        mod("\u001B[1", 'C')
                    }
                }
            }

            Key.NumPad7 -> {
                if (numLock) {
                    if (keypadApp) mod("\u001BO", 'w') else "7"
                } else {
                    // HOME
                    if (keyMod == 0) {
                        if (cursorApp) "\u001BOH" else "\u001B[H"
                    } else {
                        mod("\u001B[1", 'H')
                    }
                }
            }

            Key.NumPad8 -> {
                if (numLock) {
                    if (keypadApp) mod("\u001BO", 'x') else "8"
                } else {
                    // UP
                    if (keyMod == 0) {
                        if (cursorApp) "\u001BOA" else "\u001B[A"
                    } else {
                        mod("\u001B[1", 'A')
                    }
                }
            }

            Key.NumPad9 -> {
                if (numLock) {
                    if (keypadApp) mod("\u001BO", 'y') else "9"
                } else {
                    // PGUP
                    "\u001B[5~"
                }
            }

            Key.NumPadEquals -> if (keypadApp) mod("\u001BO", 'X') else "="

            else -> null
        }
    }

    private fun transformForModifiers(start: String, mods: Int, last: Char): String {
        val m = when (mods) {
            KeyMod.SHIFT -> 2
            KeyMod.ALT -> 3
            KeyMod.SHIFT or KeyMod.ALT -> 4
            KeyMod.CTRL -> 5
            KeyMod.SHIFT or KeyMod.CTRL -> 6
            KeyMod.ALT or KeyMod.CTRL -> 7
            KeyMod.SHIFT or KeyMod.ALT or KeyMod.CTRL -> 8
            else -> return start + last
        }
        return "$start;$m$last"
    }
}
