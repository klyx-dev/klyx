package com.klyx.editor.compose.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal actual typealias SynchronizedObject = Any

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun makeSynchronizedObject(ref: Any?) = ref ?: SynchronizedObject()

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return kotlin.synchronized(lock, block)
}
