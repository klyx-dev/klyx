package com.klyx.ui.page.main

import com.klyx.core.PlatformContext
import com.klyx.core.unsupported
import kotlin.system.exitProcess

internal actual fun openNewWindow(context: PlatformContext) {
    unsupported("New window not supported on iOS")
}

internal actual fun closeCurrentWindow(context: PlatformContext) {
    unsupported("Close window not supported on iOS")
}

internal actual fun quitApp(): Nothing = exitProcess(0)
