package com.klyx.core.logging

import kotlin.reflect.KClass

inline fun <reified T : Any> T.logger() = Logger.getLogger<T>()
fun logger(tag: String) = Logger.getLogger(tag)

class InternalLogger private constructor() {
    companion object {
        //private val mutex = Mutex()
        private var INSTANCE: InternalLogger? = null

        private val taggedLoggers = mutableMapOf<String, TaggedLogger>()

//        suspend fun getInstance(): InternalLogger {
//            return INSTANCE ?: mutex.withLock {
//                INSTANCE ?: InternalLogger().also { INSTANCE = it }
//            }
//        }

        fun getInstance(): InternalLogger {
            return INSTANCE ?: InternalLogger().also { INSTANCE = it }
        }

        fun getLogger(tag: String): TaggedLogger {
            return taggedLoggers.getOrPut(tag) {
                TaggedLogger(tag, getInstance())
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

    fun v(tag: String, message: String) = log(Level.Verbose, tag, message)
    fun d(tag: String, message: String) = log(Level.Debug, tag, message)
    fun i(tag: String, message: String) = log(Level.Info, tag, message)
    fun w(tag: String, message: String) = log(Level.Warn, tag, message)

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.Error, tag, message, throwable)
    }

    fun wtf(tag: String, message: String) = log(Level.Assert, tag, message)
}

class TaggedLogger internal constructor(
    private val tag: String,
    private val logger: InternalLogger
) {
    fun v(message: String) = logger.v(tag, message)
    fun d(message: String) = logger.d(tag, message)
    fun i(message: String) = logger.i(tag, message)
    fun w(message: String) = logger.w(tag, message)
    fun e(message: String, throwable: Throwable? = null) = logger.e(tag, message, throwable)

    fun wtf(message: String) = logger.wtf(tag, message)

    fun v(messageProvider: () -> String) = v(messageProvider())
    fun d(messageProvider: () -> String) = d(messageProvider())
    fun i(messageProvider: () -> String) = i(messageProvider())
    fun w(messageProvider: () -> String) = w(messageProvider())

    fun e(throwable: Throwable? = null, messageProvider: () -> String) {
        e(messageProvider(), throwable)
    }

    val tagName: String get() = tag
}
