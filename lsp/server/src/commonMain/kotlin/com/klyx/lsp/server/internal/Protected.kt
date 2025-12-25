package com.klyx.lsp.server.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class Protected<T>(initial: T) {
    private val mutex = Mutex()
    private var value: T = initial

    suspend fun <R> withLock(block: (T) -> R): R = mutex.withLock {
        block(value)
    }

    suspend fun update(block: (T) -> T) {
        mutex.withLock {
            value = block(value)
        }
    }
}

internal fun <T> T.protect() = Protected(this)
