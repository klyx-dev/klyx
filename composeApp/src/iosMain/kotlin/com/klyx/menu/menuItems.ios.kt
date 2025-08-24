package com.klyx.menu

import com.klyx.unsupported

internal actual fun restartApp(isKillProcess: Boolean) {
    quitApp()
}

internal actual fun openNewWindow() {
    unsupported("New window not supported on iOS")
}

internal actual fun closeCurrentWindow() {
    unsupported("Close window not supported on iOS")
}
