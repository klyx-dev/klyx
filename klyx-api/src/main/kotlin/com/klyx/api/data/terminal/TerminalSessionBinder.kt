package com.klyx.api.data.terminal

import android.content.Context
import com.klyx.core.Global
import kotlinx.coroutines.flow.StateFlow

interface TerminalSessionBinder : Global {
    val isServiceBound: StateFlow<Boolean>
    fun bind(context: Context)
    fun unbind(context: Context)
}
