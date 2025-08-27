package com.klyx.core.logging

data class LoggerConfig(
    val minimumLevel: Level = Level.Verbose,
    val enableConsoleLogging: Boolean = true,
    val enableErrorReporting: Boolean = true,
    val replayBufferSize: Int = 100,
    val bufferCapacity: Int = 1000,
    val tagFilter: (String) -> Boolean = { true },
    val messageFilter: (Message) -> Boolean = { true },
    val logFormatters: Map<Logger, LogFormatter> = emptyMap()
) {
    companion object {
        var Default = LoggerConfig()
    }
}
