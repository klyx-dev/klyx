package com.klyx.terminal

import android.content.Context
import android.content.Intent
import com.klyx.core.PlatformContext
import com.klyx.terminal.service.SessionService
import com.klyx.terminal.service.SessionServiceConnection

private class AndroidSessionBinder : SessionBinder {
    private val serviceConnection = SessionServiceConnection()

    override val isBounded = serviceConnection.isBound

    override fun bind(context: PlatformContext) {
        val intent = Intent(context, SessionService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun unbind(context: PlatformContext) {
        if (isBounded.value) {
            context.unbindService(serviceConnection)
            serviceConnection.unbind()
        }
    }
}

actual fun SessionBinder(): SessionBinder = AndroidSessionBinder()
