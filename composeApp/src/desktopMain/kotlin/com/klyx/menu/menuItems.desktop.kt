package com.klyx.menu

import kotlin.system.exitProcess

internal actual fun openSystemTerminal() {
}

internal actual fun restartApp(isKillProcess: Boolean) {
    quitApp()
}

internal actual fun quitApp(): Nothing = exitProcess(0)
