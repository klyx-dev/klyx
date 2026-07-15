package com.klyx.api.data.log

/**
 * Represents a single log event within the Klyx ecosystem.
 *
 * `LogEntry` objects are created by the [com.klyx.api.service.Logger] and are used
 * to store and display diagnostic information.
 *
 * @property timestamp The time at which the log was recorded, in milliseconds since the epoch.
 * @property level The severity level of the log entry.
 * @property tag A short string indicating the source or category of the log message.
 * @property message The primary text content of the log entry.
 * @property throwable An optional [Throwable] if an exception was associated with this log.
 * @property sourcePluginId The unique identifier of the plugin that generated this log, if applicable.
 */
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val sourcePluginId: String? = null
)
