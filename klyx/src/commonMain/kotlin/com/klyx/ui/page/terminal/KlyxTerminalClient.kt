package com.klyx.ui.page.terminal

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import com.klyx.terminal.SessionManager
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.ui.BaseTerminalClient
import com.klyx.terminal.ui.extrakeys.ExtraKeysState
import com.klyx.terminal.ui.extrakeys.SpecialButton
import kotlinx.coroutines.runBlocking

class KlyxTerminalClient(
    private val extraKeysState: ExtraKeysState,
    private val onFinishRequest: () -> Unit = {},
) : BaseTerminalClient() {
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

    override fun onKeyDown(key: Key, event: KeyEvent, session: TerminalSession): Boolean {
        if (key == Key.Enter && !session.isRunning.value) {
            runBlocking {
                SessionManager.terminate(SessionManager.currentSessionId)

                if (SessionManager.sessions.isEmpty()) {
                    onFinishRequest()
                }
            }
            return true
        }
        return super.onKeyDown(key, event, session)
    }
}
