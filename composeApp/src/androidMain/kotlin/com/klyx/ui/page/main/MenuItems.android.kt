package com.klyx.ui.page.main

import android.os.Process.SIGNAL_KILL
import android.os.Process.myPid
import android.os.Process.sendSignal
import com.klyx.activities.TerminalActivity
import com.klyx.activities.utils.launchNewWindow
import com.klyx.core.PlatformContext
import com.klyx.core.WindowManager
import com.klyx.core.openActivity
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.openTerminalTab

internal actual fun openNewWindow(context: PlatformContext) {
    with(context) { launchNewWindow() }
}

internal actual fun EditorViewModel.openSystemTerminal(
    context: PlatformContext,
    openAsTab: Boolean
) {
    if (openAsTab) {
        openTerminalTab()
    } else {
        with(context) { openActivity(TerminalActivity::class) }
    }
}

internal actual fun closeCurrentWindow(context: PlatformContext) {
    WindowManager.closeCurrentWindow()
}

internal actual fun quitApp(): Nothing {
    sendSignal(myPid(), SIGNAL_KILL)
    throw RuntimeException("sendSignal(SIGNAL_KILL) returned normally, while it was supposed to halt the process.")
}
