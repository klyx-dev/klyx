package com.klyx.ui.page.terminal

import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.ui.extrakeys.ExtraKeyButton
import com.klyx.terminal.ui.extrakeys.ExtraKeysClient

class KlyxExtraKeysClient(private val session: TerminalSession) : ExtraKeysClient {

    override suspend fun onExtraKeyButtonClick(button: ExtraKeyButton) {
        val escapeSequence = KEY_TO_ESCAPE_SEQUENCE[button.key]
        escapeSequence?.let { text ->
            session.write(text)
        } ?: run {
            session.write(button.key)
        }
    }

    companion object {
        private val KEY_TO_ESCAPE_SEQUENCE = mapOf(
            "SPACE" to " ",
            "ESC" to "\u001B",
            "TAB" to "\t",
            "ENTER" to "\r",
            "BKSP" to "\u007F", // DEL / Backspace

            "UP" to "\u001B[A",
            "DOWN" to "\u001B[B",
            "RIGHT" to "\u001B[C",
            "LEFT" to "\u001B[D",

            "HOME" to "\u001B[H",
            "END" to "\u001B[F",
            "PGUP" to "\u001B[5~",
            "PGDN" to "\u001B[6~",
            "INS" to "\u001B[2~",
            "DEL" to "\u001B[3~",

            "F1" to "\u001BOP",
            "F2" to "\u001BOQ",
            "F3" to "\u001BOR",
            "F4" to "\u001BOS",
            "F5" to "\u001B[15~",
            "F6" to "\u001B[17~",
            "F7" to "\u001B[18~",
            "F8" to "\u001B[19~",
            "F9" to "\u001B[20~",
            "F10" to "\u001B[21~",
            "F11" to "\u001B[23~",
            "F12" to "\u001B[24~"
        )
    }
}
