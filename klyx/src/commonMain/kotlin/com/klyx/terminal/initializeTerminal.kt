package com.klyx.terminal

import com.klyx.core.app.App

suspend fun initializeTerminal(app: App) {
    TerminalManager.init(app)
    app.setGlobal(SessionBinder())
}
