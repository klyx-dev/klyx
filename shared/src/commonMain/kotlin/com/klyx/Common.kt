package com.klyx

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T, R> T?.ifNull(defaultValue: () -> R): R where T : R {
    contract { callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE) }
    return this ?: defaultValue()
}

val Enum<*>.spacedName: String
    get() = name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")

fun unimplemented(exception: Throwable? = null): Nothing = throw exception.ifNull { NotImplementedError() }
fun runtimeError(message: String): Nothing = throw RuntimeException(message)
fun unsupported(message: String? = null): Nothing = throw UnsupportedOperationException(message)
