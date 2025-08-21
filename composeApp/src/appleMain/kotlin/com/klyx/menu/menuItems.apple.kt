package com.klyx.menu

import kotlin.system.exitProcess

internal actual fun openSystemTerminal() {
}

internal actual fun quitApp(): Nothing = exitProcess(0)
