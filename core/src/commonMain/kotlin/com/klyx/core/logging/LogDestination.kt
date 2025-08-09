package com.klyx.core.logging

interface LogDestination {
    fun log(message: Message)
}

class ConsoleLogDestination : LogDestination {
    override fun log(message: Message) {
        logToPlatformConsole(message)
    }
}

internal expect fun logToPlatformConsole(message: Message)
