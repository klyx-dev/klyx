package com.klyx.menu

import com.klyx.unsupported
import com.klyx.viewmodel.EditorViewModel
import kotlin.system.exitProcess

internal actual fun openSystemTerminal(viewModel: EditorViewModel, openAsTab: Boolean) {
}

internal actual fun restartApp(isKillProcess: Boolean) {
    quitApp()
}

internal actual fun quitApp(): Nothing = exitProcess(0)

internal actual fun openNewWindow() {
    unsupported("New window not supported on desktop")
}

internal actual fun closeCurrentWindow() {
    unsupported("Close window not supported on desktop")
}
