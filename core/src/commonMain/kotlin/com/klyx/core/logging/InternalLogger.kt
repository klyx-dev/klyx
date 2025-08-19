package com.klyx.core.logging

import kotlin.reflect.KClass

inline fun <reified T : Any> T.logger() = Logger.getLogger<T>()
fun logger(tag: String) = Logger.getLogger(tag)

sealed class InternalLogger : Logger {
    companion object {
        private val taggedLoggers = mutableMapOf<String, TaggedLogger>()

        fun getLogger(tag: String): TaggedLogger {
            return taggedLoggers.getOrPut(tag) {
                TaggedLogger(tag)
            }
        }

        fun getLogger(clazz: KClass<*>): TaggedLogger {
            return getLogger(clazz.simpleName ?: "Unknown")
        }

        inline fun <reified T> getLogger(): TaggedLogger {
            return getLogger(T::class)
        }
    }

    private val destinations = mutableListOf<LogDestination>()
    private var minLogLevel = Level.Verbose

    init {
        addDestination(ConsoleLogDestination())
    }

    fun addDestination(destination: LogDestination) {
        destinations.add(destination)
    }

    fun removeDestination(destination: LogDestination) {
        destinations.remove(destination)
    }

    fun setMinLogLevel(level: Level) {
        minLogLevel = level
    }

    internal fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        if (level.priority < minLogLevel.priority) return

        val logMessage = Message(level, tag, message, throwable = throwable)
        destinations.forEach { it.log(logMessage) }
    }

    override fun v(tag: String, message: String) = log(Level.Verbose, tag, message)
    override fun d(tag: String, message: String) = log(Level.Debug, tag, message)
    override fun i(tag: String, message: String) = log(Level.Info, tag, message)
    override fun w(tag: String, message: String) = log(Level.Warn, tag, message)

    override fun e(tag: String, message: String, throwable: Throwable?) {
        log(Level.Error, tag, message, throwable)
    }

    override fun wtf(tag: String, message: String) = log(Level.Assert, tag, message)
}

class TaggedLogger internal constructor(
    private val tag: String,
) : InternalLogger() {
    fun v(message: String) = v(tag, message)
    fun verbose(message: String) = v(tag, message)

    fun d(message: String) = d(tag, message)
    fun debug(message: String) = d(tag, message)

    fun i(message: String) = i(tag, message)
    fun info(message: String) = i(tag, message)

    fun w(message: String) = w(tag, message)
    fun warn(message: String) = w(tag, message)

    fun e(message: String, throwable: Throwable? = null) = e(tag, message, throwable)
    fun error(message: String, throwable: Throwable? = null) = e(tag, message, throwable)

    fun wtf(message: String) = wtf(tag, message)
    fun assert(message: String) = wtf(tag, message)

    fun v(messageProvider: () -> String) = v(messageProvider())
    fun verbose(messageProvider: () -> String) = verbose(messageProvider())

    fun d(messageProvider: () -> String) = d(messageProvider())
    fun debug(messageProvider: () -> String) = debug(messageProvider())

    fun i(messageProvider: () -> String) = i(messageProvider())
    fun info(messageProvider: () -> String) = info(messageProvider())

    fun w(messageProvider: () -> String) = w(messageProvider())
    fun warn(messageProvider: () -> String) = warn(messageProvider())

    fun e(throwable: Throwable? = null, messageProvider: () -> String) {
        e(messageProvider(), throwable)
    }

    fun error(throwable: Throwable? = null, messageProvider: () -> String) {
        error(messageProvider(), throwable)
    }

    fun wtf(messageProvider: () -> String) = wtf(messageProvider())
    fun assert(messageProvider: () -> String) = assert(messageProvider())

    val tagName: String get() = tag
}
