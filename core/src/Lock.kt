@file:OptIn(ExperimentalAtomicApi::class)

package com.klyx.core

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A short critical section, implemented as a spinlock. Suitable for map and list mutation, not for
 * anything that blocks or suspends: use [kotlinx.coroutines.sync.Mutex] for that. On JS and Wasm, where
 * execution is single threaded, the compare and set always succeeds immediately.
 *
 * Never call a method that takes this same lock from inside [withLock].
 */
class Lock {
    @PublishedApi
    internal val locked = AtomicBoolean(false)

    @IgnorableReturnValue
    inline fun <T> withLock(action: () -> T): T {
        @Suppress("ControlFlowWithEmptyBody")
        while (!locked.compareAndSet(expectedValue = false, newValue = true)) {}

        try {
            return action()
        } finally {
            locked.store(false)
        }
    }
}
