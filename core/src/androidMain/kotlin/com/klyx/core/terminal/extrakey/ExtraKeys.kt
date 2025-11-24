package com.klyx.core.terminal.extrakey

import android.view.View
import com.google.android.material.button.MaterialButton
import com.termux.shared.termux.extrakeys.ExtraKeyButton
import com.termux.shared.termux.extrakeys.ExtraKeysConstants
import com.termux.shared.termux.extrakeys.ExtraKeysInfo
import com.termux.shared.termux.extrakeys.ExtraKeysView
import com.termux.shared.termux.extrakeys.SpecialButton
import com.termux.shared.termux.extrakeys.SpecialButtonState
import com.termux.terminal.TerminalSession

typealias ExtraKeysView = ExtraKeysView
typealias ExtraKeysInfo = ExtraKeysInfo
typealias ExtraKeysConstants = ExtraKeysConstants
typealias ExtraKeyButton = ExtraKeyButton

typealias SpecialButton = SpecialButton
typealias SpecialButtonState = SpecialButtonState

class ExtraKeysViewClient(
    private val session: TerminalSession
) : ExtraKeysView.IExtraKeysView {
    override fun onExtraKeyButtonClick(
        view: View,
        buttonInfo: ExtraKeyButton,
        button: MaterialButton
    ) {
        val escapeSequence = KEY_TO_ESCAPE_SEQUENCE[buttonInfo.key]
        escapeSequence?.let { text ->
            session.write(text)
        } ?: run {
            session.write(buttonInfo.key)
        }
    }

    override fun performExtraKeyButtonHapticFeedback(
        view: View,
        buttonInfo: ExtraKeyButton,
        button: MaterialButton
    ) = true

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
