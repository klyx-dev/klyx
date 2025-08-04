package com.klyx.terminal

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.termux.shared.shell.command.runner.app.AppShell
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession

class TerminalService : Service(), AppShell.AppShellClient, TermuxSession.TermuxSessionClient {
    inner class LocalBinder : Binder() {
        val service = this@TerminalService
    }

    private val mBinder = LocalBinder()
    private val mHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    /**
     * Callback function for when [AppShell] exits.
     *
     * @param appShell The [AppShell] that exited.
     */
    override fun onAppShellExited(appShell: AppShell?) {
        TODO("Not yet implemented")
    }

    /**
     * Callback function for when [TermuxSession] exits.
     *
     * @param termuxSession The [TermuxSession] that exited.
     */
    override fun onTermuxSessionExited(termuxSession: TermuxSession?) {
        TODO("Not yet implemented")
    }
}
