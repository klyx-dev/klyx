package com.klyx.core.logging

actual object ConsoleLogger : Logger {
    private val formatter = DefaultLogFormatter()

    actual override fun log(message: Message) {
        val formattedMessage = formatter.format(message)
        when (message.level) {
            Level.Error, Level.Assert -> System.err.println(formattedMessage)
            else -> println(formattedMessage)
        }
    }
}
