package com.klyx.core.logging

import kotlin.reflect.KClass

object Logger {
    private val logger = InternalLogger.getInstance()

    init {
        logger.addDestination(ConsoleLogDestination())
    }

    fun setMinLogLevel(level: Level) = logger.setMinLogLevel(level)

    fun v(tag: String, message: String) = logger.v(tag, message)
    fun d(tag: String, message: String) = logger.d(tag, message)
    fun i(tag: String, message: String) = logger.i(tag, message)
    fun w(tag: String, message: String) = logger.w(tag, message)

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        logger.e(tag, message, throwable)
    }

    fun wtf(tag: String, message: String) = logger.wtf(tag, message)

    fun getLogger(tag: String) = InternalLogger.getLogger(tag)
    fun getLogger(clazz: KClass<*>) = InternalLogger.getLogger(clazz)

    inline fun <reified T> getLogger() = InternalLogger.getLogger<T>()

    fun addDestination(destination: LogDestination) = logger.addDestination(destination)
    fun removeDestination(destination: LogDestination) = logger.removeDestination(destination)
}
