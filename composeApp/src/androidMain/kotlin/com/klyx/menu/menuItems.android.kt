package com.klyx.menu

import android.os.Process.SIGNAL_KILL
import android.os.Process.myPid
import android.os.Process.sendSignal
import com.blankj.utilcode.util.AppUtils
import com.klyx.activities.TerminalActivity
import com.klyx.core.ContextHolder
import com.klyx.core.openActivity

internal actual fun openSystemTerminal() {
    with(ContextHolder.mainActivityContextRef.get()) {
        openActivity(TerminalActivity::class)
    }
}

internal actual fun restartApp(isKillProcess: Boolean) {
    AppUtils.relaunchApp(isKillProcess)
}

internal actual fun quitApp(): Nothing {
    sendSignal(myPid(), SIGNAL_KILL)
    throw RuntimeException("sendSignal(SIGNAL_KILL) returned normally, while it was supposed to halt the process.")
}
