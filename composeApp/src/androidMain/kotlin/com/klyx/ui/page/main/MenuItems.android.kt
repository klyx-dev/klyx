package com.klyx.ui.page.main

import com.klyx.activities.utils.launchNewWindow
import com.klyx.core.PlatformContext
import com.klyx.core.WindowManager

internal actual fun openNewWindow(context: PlatformContext) = context.launchNewWindow()

internal actual fun closeCurrentWindow(context: PlatformContext) = WindowManager.closeCurrentWindow()

internal actual fun quitApp(): Nothing = WindowManager.closeAllWindowsAndQuit()
