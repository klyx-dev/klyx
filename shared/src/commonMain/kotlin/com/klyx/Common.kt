package com.klyx

val Enum<*>.spacedName: String
    get() = name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")

fun unimplemented(exception: Throwable? = null): Nothing = throw exception ?: NotImplementedError()
fun runtimeError(message: String): Nothing = throw RuntimeException(message)
fun unsupported(message: String? = null): Nothing = throw UnsupportedOperationException(message)

fun unreachable(): Nothing = throw IllegalStateException("Unreachable code")
fun unreachable(message: String): Nothing = throw IllegalStateException(message)
