package com.klyx

val Enum<*>.spacedName: String
    get() = name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")

fun unimplemented(exception: Throwable? = null): Nothing = throw exception ?: NotImplementedError()
fun unsupported(message: String? = null): Nothing = throw UnsupportedOperationException(message)

fun unreachable(message: String? = null): Nothing = error(message ?: "Unreachable code")
