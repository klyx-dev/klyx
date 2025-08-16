package com.klyx.core.logging

import kotlin.reflect.KClass

sealed interface Logger {
    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)

    fun e(tag: String, message: String, throwable: Throwable? = null)

    fun wtf(tag: String, message: String)

    companion object
}

inline fun <reified T> Logger.Companion.getLogger() = InternalLogger.getLogger<T>()
fun Logger.Companion.getLogger(tag: String) = InternalLogger.getLogger(tag)
fun Logger.Companion.getLogger(clazz: KClass<*>) = InternalLogger.getLogger(clazz)
