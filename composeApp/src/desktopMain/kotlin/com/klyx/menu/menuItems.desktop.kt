package com.klyx.menu

import com.klyx.unsupported
import kotlin.system.exitProcess

internal actual fun openSystemTerminal() {
}

internal actual fun restartApp(isKillProcess: Boolean) {
    quitApp()
}

internal actual fun quitApp(): Nothing = exitProcess(0)

internal actual fun openNewWindow() {
    unsupported("New window not supported on desktop")
}
