package com.klyx.terminal.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SessionServiceConnection : ServiceConnection {
    var isBound = false
    var sessionBinder by mutableStateOf<SessionService.SessionBinder?>(null)

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as SessionService.SessionBinder
        sessionBinder = binder
        isBound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        isBound = false
        sessionBinder = null
    }
}
