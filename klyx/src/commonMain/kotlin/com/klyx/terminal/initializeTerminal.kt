package com.klyx.terminal

import com.klyx.core.app.App
import com.klyx.core.terminal.TerminalManager

suspend fun initializeTerminal(app: App) {
    TerminalManager.init(app)
    app.setGlobal(SessionBinder())
}
