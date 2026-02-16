package com.klyx.terminal.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SessionServiceConnection : ServiceConnection {
    val isBound: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val sessionBinder: StateFlow<SessionService.SessionBinder?>
        field = MutableStateFlow(null)

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as SessionService.SessionBinder
        sessionBinder.update { binder }
        isBound.update { true }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        unbind()
    }

    fun unbind() {
        isBound.update { false }
        sessionBinder.update { null }
    }
}
