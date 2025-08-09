package com.klyx.core.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal actual fun logToPlatformConsole(message: Message) {
    val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        .format(Date(message.timestamp))

    val levelTag = when (message.level) {
        Level.Verbose -> "V"
        Level.Debug -> "D"
        Level.Info -> "I"
        Level.Warn -> "W"
        Level.Error -> "E"
        Level.Assert -> "A"
    }

    println("[$timestamp] $levelTag/${message.tag}: ${message.message}")
    message.throwable?.printStackTrace()
}
