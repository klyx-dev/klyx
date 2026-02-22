package com.klyx.ui.page.terminal

import com.klyx.terminal.ui.BaseTerminalClient
import com.klyx.terminal.ui.extrakeys.ExtraKeysState
import com.klyx.terminal.ui.extrakeys.SpecialButton

class KlyxTerminalClient(private val extraKeysState: ExtraKeysState) : BaseTerminalClient() {
    override fun readControlKey(): Boolean {
        return extraKeysState.readSpecialButton(SpecialButton.Ctrl) ?: false
    }

    override fun readAltKey(): Boolean {
        return extraKeysState.readSpecialButton(SpecialButton.Alt) ?: false
    }

    override fun readFnKey(): Boolean {
        return extraKeysState.readSpecialButton(SpecialButton.Fn) ?: false
    }

    override fun readShiftKey(): Boolean {
        return extraKeysState.readSpecialButton(SpecialButton.Shift) ?: false
    }
}
