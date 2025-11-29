@file:OptIn(ExperimentalTime::class)

package com.klyx.core.logging

import com.github.michaelbull.result.onFailure
import com.klyx.core.AnyResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.ExperimentalTime

val defaultLogger = logger("Klyx")
inline val log get() = defaultLogger

fun <A> AnyResult<A>.logErr(): AnyResult<A> {
    onFailure { log.error { it.toString() } }
    return this
}

class KxLogger private constructor(
    @PublishedApi
    internal val tag: String,
    private val globalConfig: LoggerConfig = LoggerConfig.Default
) : Logger {
    private val loggers = mutableListOf<Logger>()

    private val _logFlow = MutableSharedFlow<Message>(
        replay = globalConfig.replayBufferSize,
        extraBufferCapacity = globalConfig.bufferCapacity
    )

    val logFlow: Flow<Message> = _logFlow.asSharedFlow()

    init {
        if (globalConfig.enableConsoleLogging) {
            loggers.add(ConsoleLogger)
        }

        loggers.add(FlowLogger(_logFlow))
    }

    override fun log(message: Message) {
        if (message.level.priority < globalConfig.minimumLevel.priority) {
            return
        }

        if (!globalConfig.tagFilter(message.tag) || !globalConfig.messageFilter(message)) {
            return
        }

        for (logger in loggers) {
            try {
                logger.log(message)
            } catch (_: Exception) {
                if (globalConfig.enableErrorReporting) {
                    //System.err.println("Logger error: ${e.message}")
                }
            }
        }
    }

    fun verbose(message: String?, metadata: Map<String, Any> = emptyMap()) {
        log(Message(Level.Verbose, tag, message.toString(), metadata = metadata))
    }

    fun debug(message: String?, metadata: Map<String, Any> = emptyMap()) {
        log(Message(Level.Debug, tag, message.toString(), metadata = metadata))
    }

    fun info(message: String?, metadata: Map<String, Any> = emptyMap()) {
        log(Message(Level.Info, tag, message.toString(), metadata = metadata))
    }

    fun warn(message: String?, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(Message(Level.Warning, tag, message.toString(), throwable = throwable, metadata = metadata))
    }

    fun error(message: String?, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(Message(Level.Error, tag, message.toString(), throwable = throwable, metadata = metadata))
    }

    fun assert(message: String?, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(Message(Level.Assert, tag, message.toString(), throwable = throwable, metadata = metadata))
    }

    inline fun verbose(crossinline message: () -> String?) {
        if (shouldLog(Level.Verbose)) verbose(message())
    }

    inline fun debug(crossinline message: () -> String?) {
        if (shouldLog(Level.Debug)) debug(message())
    }

    inline fun info(crossinline message: () -> String?) {
        if (shouldLog(Level.Info)) info(message())
    }

    inline fun warn(throwable: Throwable? = null, crossinline message: () -> String?) {
        if (shouldLog(Level.Warning)) warn(message(), throwable)
    }

    inline fun error(throwable: Throwable? = null, crossinline message: () -> String?) {
        if (shouldLog(Level.Error)) error(message(), throwable)
    }

    inline fun assert(throwable: Throwable? = null, crossinline message: () -> String?) {
        if (shouldLog(Level.Assert)) assert(message(), throwable)
    }

    fun logStructured(
        level: Level,
        message: String,
        vararg fields: Pair<String, Any>,
        throwable: Throwable? = null
    ) {
        log(Message(level, tag, message, throwable = throwable, metadata = fields.toMap()))
    }

    fun log(
        level: Level,
        message: String,
        vararg fields: Pair<String, Any>,
        throwable: Throwable? = null
    ) = log(Message(level, tag, message, throwable = throwable, metadata = fields.toMap()))

    inline fun <T> measure(
        operationName: String,
        level: Level = Level.Debug,
        block: () -> T
    ): T {
        val startTime = kotlin.time.Clock.System.now()
        val result = try {
            block()
        } catch (e: Exception) {
            val duration = kotlin.time.Clock.System.now() - startTime
            log(
                Message(
                    Level.Error,
                    tag,
                    "Operation '$operationName' failed after ${duration.inWholeMilliseconds}ms",
                    throwable = e,
                    metadata = mapOf(
                        "operation" to operationName,
                        "duration_ms" to duration.inWholeMilliseconds,
                        "success" to false
                    )
                )
            )
            throw e
        }

        val duration = kotlin.time.Clock.System.now() - startTime
        log(
            Message(
                level,
                tag,
                "Operation '$operationName' completed in ${duration.inWholeMilliseconds}ms",
                metadata = mapOf(
                    "operation" to operationName,
                    "duration_ms" to duration.inWholeMilliseconds,
                    "success" to true
                )
            )
        )

        return result
    }

    fun progress(
        message: String,
        percentage: Int? = null,
        token: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val progressData = mutableMapOf<String, Any>()
        percentage?.let { progressData["percentage"] = it }
        token?.let { progressData["token"] = it }
        log(Message(Level.Info, tag, message, metadata = metadata + progressData, type = MessageType.Progress))
    }

    inline fun progress(percentage: Int? = null, token: String? = null, crossinline message: () -> String) {
        if (shouldLog(Level.Info)) {
            progress(message(), percentage, token)
        }
    }

    @PublishedApi
    internal fun shouldLog(level: Level): Boolean {
        return level.priority >= globalConfig.minimumLevel.priority
    }

    fun addLogger(logger: Logger) {
        if (logger !in loggers) {
            loggers.add(logger)
        }
    }

    companion object {
        fun getLogger(tag: String, config: LoggerConfig = LoggerConfig.Default): KxLogger {
            return loggers.getOrPut(tag) { KxLogger(tag, config) }.also {
                it.addLogger(KxLog.globalFlowLogger)
            }
        }

        fun configure(config: LoggerConfig) {
            LoggerConfig.Default = config
        }
    }
}

inline fun <reified T> T.logger(): KxLogger = KxLog.logger<T>()

@Suppress("NOTHING_TO_INLINE")
inline fun logger(tag: String): KxLogger = KxLog.logger(tag)

inline fun <reified T> T.logV(message: String, metadata: Map<String, Any> = emptyMap()) {
    logger<T>().verbose(message, metadata)
}

inline fun <reified T> T.logD(message: String, metadata: Map<String, Any> = emptyMap()) {
    logger<T>().debug(message, metadata)
}

inline fun <reified T> T.logI(message: String, metadata: Map<String, Any> = emptyMap()) {
    logger<T>().info(message, metadata)
}

inline fun <reified T> T.logW(message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
    logger<T>().warn(message, throwable, metadata)
}

inline fun <reified T> T.logE(message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
    logger<T>().error(message, throwable, metadata)
}

inline fun <reified T> T.logV(crossinline message: () -> String) {
    logger<T>().verbose(message)
}

inline fun <reified T> T.logD(crossinline message: () -> String) {
    logger<T>().debug(message)
}

inline fun <reified T> T.logI(crossinline message: () -> String) {
    logger<T>().info(message)
}

inline fun <reified T> T.logW(throwable: Throwable? = null, crossinline message: () -> String) {
    logger<T>().warn(throwable, message)
}

inline fun <reified T> T.logE(throwable: Throwable? = null, crossinline message: () -> String) {
    logger<T>().error(throwable, message)
}

inline fun <reified T> T.logProgress(
    message: String,
    percentage: Int? = null,
    token: String? = null,
    metadata: Map<String, Any> = emptyMap()
) {
    logger<T>().progress(message, percentage, token, metadata)
}
