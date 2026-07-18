package com.klyx.api.service

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.klyx.api.data.log.LogEntry
import com.klyx.api.data.log.LogLevel
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService
import com.klyx.api.plugin.info
import com.klyx.api.plugin.pluginService
import com.klyx.core.LocalApp
import kotlinx.coroutines.flow.StateFlow

/**
 * Marker interface for values that can serve as an implicit logging context
 * (e.g. the currently executing plugin).
 *
 * Bringing a [LogContext] instance into scope via `with(...)` (or as an explicit
 * `context(...)` parameter) allows the context-aware logging extensions on [Logger]
 * (such as [Logger.trace], [Logger.debug], [Logger.info], [Logger.warn], and
 * [Logger.error]) to automatically derive a tag and, when applicable, a source
 * plugin id, without requiring the caller to pass them explicitly.
 *
 * Implement this interface on any type that should be usable as an implicit
 * logging context, such as [KlyxPlugin]. For call sites with no meaningful
 * context, use [NoLogContext].
 *
 * @see NoLogContext
 * @see Logger
 */
interface LogContext

/**
 * A [LogContext] representing the absence of a specific logging context.
 *
 * Use this when logging from code that isn't associated with a plugin or other
 * identifiable source. Log entries produced under this context are tagged as
 * `"<anonymous>"`.
 *
 * @see LogContext
 */
object NoLogContext : LogContext

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
 * Logs a message for the current [LogContext].
 *
 * The tag is derived from [ctx]: if it's a [KlyxPlugin], the plugin's descriptor
 * name is used and its id is recorded as the source plugin id; if it's [NoLogContext],
 * the tag `"<anonymous>"` is used; otherwise the simple class name of [ctx] is used.
 *
 * This extension function automatically uses the plugin's name from its descriptor as the tag,
 * ensuring consistent logging across plugins.
 *
 * @param level The severity level of the log.
 * @param message The message to be logged.
 * @param throwable An optional [Throwable] associated with the log entry.
 */
context(ctx: LogContext)
fun Logger.log(level: LogLevel, message: String, throwable: Throwable? = null) {
    val tag = when (ctx) {
        is KlyxPlugin -> ctx.info.descriptor.name
        NoLogContext -> "<anonymous>"
        else -> ctx::class.simpleName ?: "<anonymous>"
    }
    val sourcePluginId = (ctx as? KlyxPlugin)?.info?.id
    log(level, tag, message, throwable, sourcePluginId = sourcePluginId)
}

/**
 * Logs a [LogLevel.TRACE] message for the current [LogContext].
 * Use for extremely fine-grained diagnostic information.
 *
 * @param message The message to be logged.
 */
context(ctx: LogContext)
fun Logger.trace(message: String) = log(LogLevel.TRACE, message)

/**
 * Logs a [LogLevel.DEBUG] message for the current [LogContext].
 * Use for information useful during development and troubleshooting.
 *
 * @param message The message to be logged.
 */
context(ctx: LogContext)
fun Logger.debug(message: String) = log(LogLevel.DEBUG, message)

/**
 * Logs a [LogLevel.INFO] message for the current [LogContext].
 * Use for highlighting the progress of the application at a coarse-grained level.
 *
 * @param message The message to be logged.
 */
context(ctx: LogContext)
fun Logger.info(message: String) = log(LogLevel.INFO, message)

/**
 * Logs a [LogLevel.WARN] message for the current [LogContext].
 * Use for potentially harmful situations or non-critical failures.
 *
 * @param message The message to be logged.
 */
context(ctx: LogContext)
fun Logger.warn(message: String) = log(LogLevel.WARN, message)

/**
 * Logs a [LogLevel.ERROR] message for the current [LogContext].
 * Use for error events that might still allow the application to continue running.
 *
 * @param message The message to be logged.
 * @param throwable An optional [Throwable] associated with the error.
 */
context(ctx: LogContext)
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

/**
 * Remembers and returns the [Logger] service from the current [LocalApp].
 *
 * @return The [Logger] service.
 */
@Composable
fun rememberLogger(): Logger {
    val app = LocalApp.current
    return remember(app) { app.pluginService(Logger::class) }
}
