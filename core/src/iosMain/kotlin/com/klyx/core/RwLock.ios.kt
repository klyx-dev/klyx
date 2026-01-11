package com.klyx.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.pthread_rwlock_destroy
import platform.posix.pthread_rwlock_init
import platform.posix.pthread_rwlock_rdlock
import platform.posix.pthread_rwlock_t
import platform.posix.pthread_rwlock_unlock
import platform.posix.pthread_rwlock_wrlock

@OptIn(ExperimentalForeignApi::class)
actual class RwLock<T> actual constructor(initial: T) : AutoCloseable {

    private val rwlock = nativeHeap.alloc<pthread_rwlock_t>()
    private var value: T = initial

    init {
        pthread_rwlock_init(rwlock.ptr, null)
    }

    actual fun read(): T {
        pthread_rwlock_rdlock(rwlock.ptr)
        try {
            return value
        } finally {
            pthread_rwlock_unlock(rwlock.ptr)
        }
    }

    actual fun write(block: (T) -> T) {
        pthread_rwlock_wrlock(rwlock.ptr)
        try {
            value = block(value)
        } finally {
            pthread_rwlock_unlock(rwlock.ptr)
        }
    }

    @IgnorableReturnValue
    actual override fun close() {
        pthread_rwlock_destroy(rwlock.ptr)
        nativeHeap.free(rwlock.rawPtr)
    }
}
