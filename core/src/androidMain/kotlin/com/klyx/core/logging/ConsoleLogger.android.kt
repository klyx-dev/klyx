package com.klyx.core.logging

import android.util.Log as AndroidLog

actual object ConsoleLogger : Logger {
    private val formatter = DefaultLogFormatter(includeTimestamp = false)

    actual override fun log(message: Message) {
        val formattedMessage = formatter.format(message)
        when (message.level) {
            Level.Verbose -> AndroidLog.v(message.tag, formattedMessage, message.throwable)
            Level.Debug -> AndroidLog.d(message.tag, formattedMessage, message.throwable)
            Level.Info -> AndroidLog.i(message.tag, formattedMessage, message.throwable)
            Level.Warning -> AndroidLog.w(message.tag, formattedMessage, message.throwable)
            Level.Error -> AndroidLog.e(message.tag, formattedMessage, message.throwable)
            Level.Assert -> AndroidLog.wtf(message.tag, formattedMessage, message.throwable)
        }
    }
}
