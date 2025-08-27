package com.klyx.core.logging

import platform.Foundation.NSLog

actual object ConsoleLogger : Logger {
    private val formatter = DefaultLogFormatter()

    actual override fun log(message: Message) {
        val formattedMessage = formatter.format(message)
        NSLog("[${message.level.displayName}] %@", formattedMessage)
    }
}
