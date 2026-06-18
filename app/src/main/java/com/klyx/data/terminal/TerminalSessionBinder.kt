package com.klyx.data.terminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.klyx.core.Global
import com.klyx.platform.service.TerminalService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Abstraction over binding to the terminal service.
 *
 * Implementations are responsible for connecting / disconnecting to the
 * underlying service that hosts terminal sessions and exposing a reactive
 * [isServiceBound] state that the UI can observe.
 */
interface TerminalSessionBinder : Global {
    val isServiceBound: StateFlow<Boolean>

    fun bind(context: Context)
    fun unbind(context: Context)
}

/**
 * Default implementation of [TerminalSessionBinder] that binds to
 * [TerminalService] via [Context.bindService].
 */
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
                // Service not registered. already unbound or context mismatch
            }
            _binder.update { null }
        }
    }
}
