package com.klyx.data.terminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.klyx.core.Global
import com.klyx.platform.service.TerminalService
import com.klyx.platform.service.TerminalService.LocalBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SessionBinder : Global {
    val isServiceBound: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val binder: StateFlow<LocalBinder?>
        field = MutableStateFlow(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder.update { service as LocalBinder }
            isServiceBound.update { true }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder.update { null }
            isServiceBound.update { false }
        }
    }

    fun bind(context: Context) {
        val intent = Intent(context, TerminalService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        if (isServiceBound.value) {
            context.unbindService(connection)
            binder.update { null }
            isServiceBound.update { false }
        }
    }
}
