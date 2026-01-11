package com.klyx.core

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

expect class RwLock<T>(initial: T) : AutoCloseable {
    fun read(): T
    fun write(block: (T) -> T)

    @IgnorableReturnValue
    override fun close()
}

inline fun <T, R> RwLock<T>.withLock(block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block(read())
}
