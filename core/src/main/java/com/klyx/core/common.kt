@file:OptIn(ExperimentalContracts::class)

package com.klyx.core

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <T, R> T?.ifNull(defaultValue: () -> R): R where T : R {
    contract { callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE) }
    return this ?: defaultValue()
}

val Enum<*>.spacedName: String
    get() = name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
