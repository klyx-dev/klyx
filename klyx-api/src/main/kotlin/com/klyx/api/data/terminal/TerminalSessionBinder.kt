package com.klyx.api.data.terminal

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface TerminalSessionBinder {
    val isServiceBound: StateFlow<Boolean>
    fun bind(context: Context)
    fun unbind(context: Context)
}
