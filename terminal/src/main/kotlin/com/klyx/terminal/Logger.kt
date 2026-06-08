package com.klyx.terminal

import android.util.Log
import com.klyx.terminal.emulator.TerminalSessionClient
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object Logger {
    fun logError(
        client: TerminalSessionClient?,
        logTag: String,
        message: String?,
        cause: Throwable? = null
    ) {
        client?.logError(logTag, message) ?: Log.e(logTag, message ?: "", cause)
    }

    fun logWarn(client: TerminalSessionClient?, logTag: String, message: String?) {
        client?.logWarn(logTag, message) ?: Log.w(logTag, message ?: "")
    }

    fun logInfo(client: TerminalSessionClient?, logTag: String, message: String?) {
        client?.logInfo(logTag, message) ?: Log.i(logTag, message ?: "")
    }

    fun logDebug(client: TerminalSessionClient?, logTag: String, message: String?) {
        client?.logDebug(logTag, message) ?: Log.d(logTag, message ?: "")
    }

    fun logVerbose(client: TerminalSessionClient?, logTag: String, message: String?) {
        client?.logVerbose(logTag, message) ?: Log.v(logTag, message ?: "")
    }

    fun logStackTraceWithMessage(
        client: TerminalSessionClient?,
        logTag: String,
        message: String?,
        throwable: Throwable?
    ) {
        logError(client, logTag, getMessageAndStackTraceString(message, throwable), throwable)
    }

    @OptIn(ExperimentalContracts::class)
    fun getMessageAndStackTraceString(message: String?, throwable: Throwable?): String? {
        contract { returnsNotNull() implies (throwable != null || message != null) }
        return if (message == null && throwable == null) null
        else if (message != null && throwable != null) message + ":\n" + throwable.stackTraceToString()
        else throwable?.stackTraceToString() ?: message
    }
}
