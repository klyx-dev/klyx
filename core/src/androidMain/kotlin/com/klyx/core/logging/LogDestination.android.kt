package com.klyx.core.logging

import android.util.Log

internal actual fun logToPlatformConsole(message: Message) {
    val fullMessage = formatLogMessage(message)
    when (message.level) {
        Level.Verbose -> Log.v(message.tag, fullMessage, message.throwable)
        Level.Debug -> Log.d(message.tag, fullMessage, message.throwable)
        Level.Info -> Log.i(message.tag, fullMessage, message.throwable)
        Level.Warn -> Log.w(message.tag, fullMessage, message.throwable)
        Level.Error -> Log.e(message.tag, fullMessage, message.throwable)
        Level.Assert -> Log.wtf(message.tag, fullMessage, message.throwable)
    }
}

private fun formatLogMessage(message: Message): String {
    return message.message + (message.throwable?.let { "\n${it.stackTraceToString()}" } ?: "")
}
