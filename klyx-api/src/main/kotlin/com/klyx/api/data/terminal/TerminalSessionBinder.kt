package com.klyx.api.data.terminal

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the binding lifecycle between the application and the background terminal service.
 *
 * The terminal service runs as an Android Service to ensure that terminal processes
 * continue to run even when the UI is not in the foreground. This binder facilitates
 * the connection required to interact with those processes.
 */
interface TerminalSessionBinder {

    /**
     * A [StateFlow] indicating whether the application is currently bound to the terminal service.
     */
    val isServiceBound: StateFlow<Boolean>

    /**
     * Initiates binding to the terminal service.
     *
     * @param context The context used to call `bindService`.
     */
    fun bind(context: Context)

    /**
     * Disconnects from the terminal service.
     *
     * @param context The context used to call `unbindService`.
     */
    fun unbind(context: Context)
}
