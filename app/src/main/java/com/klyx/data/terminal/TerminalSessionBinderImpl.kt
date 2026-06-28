package com.klyx.data.terminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.klyx.api.data.terminal.TerminalSessionBinder
import com.klyx.platform.service.TerminalService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TerminalSessionBinderImpl : TerminalSessionBinder {
    private val _isServiceBound = MutableStateFlow(false)
    override val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private val _binder = MutableStateFlow<TerminalService.LocalBinder?>(null)
    val binder: StateFlow<TerminalService.LocalBinder?> = _binder.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            _binder.update { service as TerminalService.LocalBinder }
            _isServiceBound.update { true }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _binder.update { null }
            _isServiceBound.update { false }
        }
    }

    override fun bind(context: Context) {
        val intent = Intent(context, TerminalService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun unbind(context: Context) {
        if (_isServiceBound.compareAndSet(expect = true, update = false)) {
            try {
                context.unbindService(connection)
            } catch (_: IllegalArgumentException) {
            }
            _binder.update { null }
        }
    }
}
