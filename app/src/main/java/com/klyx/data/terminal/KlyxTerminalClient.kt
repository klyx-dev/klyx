package com.klyx.data.terminal

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.ui.BaseTerminalClient
import com.klyx.terminal.ui.extrakeys.ExtraKeysState
import com.klyx.terminal.ui.extrakeys.SpecialButton
import kotlinx.coroutines.runBlocking

@OptIn(UnsafeGlobalAccess::class)
class KlyxTerminalClient(
    private val extraKeysState: ExtraKeysState,
    private val onFinishRequest: () -> Unit = {},
    private val sessionManager: TerminalSessionManager = GlobalApp.global(),
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
                sessionManager.terminateCurrentSession()

                if (sessionManager.sessions.value.isEmpty()) {
                    onFinishRequest()
                }
            }
            return true
        }
        return super.onKeyDown(key, event, session)
    }
}
