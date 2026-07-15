package com.klyx.api.service

import com.klyx.api.data.log.LogEntry
import com.klyx.api.data.log.LogLevel
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService
import com.klyx.api.plugin.info
import kotlinx.coroutines.flow.StateFlow

/**
 * Service responsible for logging events and messages within the Klyx ecosystem.
 *
 * ### Accessing the Logger
 *
 * 1. **Using the `plugin()` delegate (Recommended):**
 *    ```kotlin
 *    val logger: Logger by plugin()
 *    logger.i("MyTag", "Hello World")
 *    ```
 *
 * 2. **Context-aware logging within a Plugin:**
 *    If you have a `KlyxPlugin` in context, you can use the concise extension:
 *    ```kotlin
 *    with(plugin) {
 *        logger.info("Something happened") // Automatically tags with plugin name
 *    }
 *    ```
 *
 * @see LogEntry
 * @see LogLevel
 * @see PluginService
 */
interface Logger : PluginService {

    /**
     * A [StateFlow] emitting the list of all recorded [LogEntry]s.
     *
     * @see LogEntry
     */
    val entries: StateFlow<List<LogEntry>>

    /**
     * Logs a message with the specified [level] and [tag].
     *
     * @param level The severity level of the log (TRACE to ERROR).
     * @param tag A string identifying the source of the log message, usually the class name.
     * @param message The message to be logged.
     * @param throwable An optional [Throwable] to record an exception's stack trace.
     * @param sourcePluginId An optional identifier for the plugin that generated this log.
     */
    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        sourcePluginId: String? = null
    )

    /**
     * Clears all currently stored log entries from the in-memory buffer.
     */
    fun clear()
}

/**
 * A wrapper for [Logger] that automatically associates all logs with a specific plugin.
 */
class PluginLogger(
    private val delegate: Logger,
    private val pluginId: String
) : Logger by delegate {

    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        sourcePluginId: String?
    ) {
        delegate.log(level, tag, message, throwable, sourcePluginId ?: pluginId)
    }
}

/**
 * Logs a message for a specific [KlyxPlugin].
 *
 * This extension function automatically uses the plugin's name from its descriptor as the tag,
 * ensuring consistent logging across plugins.
 *
 * @param level The severity level of the log.
 * @param message The message to be logged.
 * @param throwable An optional [Throwable] associated with the log entry.
 */
context(plugin: KlyxPlugin)
fun Logger.log(level: LogLevel, message: String, throwable: Throwable? = null) {
    val tag = plugin.info.descriptor.name
    log(level, tag, message, throwable, sourcePluginId = plugin.info.id)
}

/**
 * Logs a [LogLevel.TRACE] message for the current plugin context.
 */
context(plugin: KlyxPlugin)
fun Logger.trace(message: String) = log(LogLevel.TRACE, message)

/**
 * Logs a [LogLevel.DEBUG] message for the current plugin context.
 */
context(plugin: KlyxPlugin)
fun Logger.debug(message: String) = log(LogLevel.DEBUG, message)

/**
 * Logs a [LogLevel.INFO] message for the current plugin context.
 */
context(plugin: KlyxPlugin)
fun Logger.info(message: String) = log(LogLevel.INFO, message)

/**
 * Logs a [LogLevel.WARN] message for the current plugin context.
 */
context(plugin: KlyxPlugin)
fun Logger.warn(message: String) = log(LogLevel.WARN, message)

/**
 * Logs a [LogLevel.ERROR] message for the current plugin context.
 */
context(plugin: KlyxPlugin)
fun Logger.error(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)

/**
 * Logs a [LogLevel.TRACE] message.
 * Use for extremely fine-grained diagnostic information.
 */
fun Logger.trace(tag: String, message: String) = log(LogLevel.TRACE, tag, message)

/**
 * Logs a [LogLevel.DEBUG] message.
 * Use for information useful during development and troubleshooting.
 */
fun Logger.debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)

/**
 * Logs a [LogLevel.INFO] message.
 * Use for highlighting the progress of the application at a coarse-grained level.
 */
fun Logger.info(tag: String, message: String) = log(LogLevel.INFO, tag, message)

/**
 * Logs a [LogLevel.WARN] message.
 * Use for potentially harmful situations or non-critical failures.
 */
fun Logger.warn(tag: String, message: String) = log(LogLevel.WARN, tag, message)

/**
 * Logs a [LogLevel.ERROR] message.
 * Use for error events that might still allow the application to continue running.
 *
 * @param tag A string identifying the source of the log message.
 * @param message The message to be logged.
 * @param throwable An optional [Throwable] associated with the error.
 */
fun Logger.error(tag: String, message: String, throwable: Throwable? = null) =
    log(LogLevel.ERROR, tag, message, throwable)
