package com.klyx.core

import java.util.concurrent.locks.ReentrantReadWriteLock

actual class RwLock<T> actual constructor(initial: T) : AutoCloseable {

    private val lock = ReentrantReadWriteLock()
    private val readLock = lock.readLock()
    private val writeLock = lock.writeLock()

    private var value: T = initial

    actual fun read(): T {
        readLock.lock()
        try {
            return value
        } finally {
            readLock.unlock()
        }
    }

    actual fun write(block: (T) -> T) {
        writeLock.lock()
        try {
            value = block(value)
        } finally {
            writeLock.unlock()
        }
    }

    actual override fun close() {
        //
    }
}
