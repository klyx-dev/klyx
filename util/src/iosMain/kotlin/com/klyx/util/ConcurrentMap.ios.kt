package com.klyx.util

import kotlinx.cinterop.Arena
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock

@OptIn(ExperimentalForeignApi::class)
actual class ConcurrentMap<K, V> actual constructor() : MutableMap<K, V> {
    private val delegate = HashMap<K, V>()

    private val arena = Arena()
    private val mutex = arena.alloc<pthread_mutex_t>()

    init {
        pthread_mutex_init(mutex.ptr, null)
    }

    private inline fun <T> withLock(block: () -> T): T {
        pthread_mutex_lock(mutex.ptr)
        try {
            return block()
        } finally {
            pthread_mutex_unlock(mutex.ptr)
        }
    }

    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = withLock { delegate.entries.toMutableSet() }
    actual override val keys: MutableSet<K>
        get() = withLock { delegate.keys.toMutableSet() }
    actual override val values: MutableCollection<V>
        get() = withLock { delegate.values.toMutableList() }

    actual override val size: Int get() = withLock { delegate.size }

    actual override fun clear() = withLock { delegate.clear() }
    actual override fun put(key: K, value: V) = withLock { delegate.put(key, value) }
    actual override fun putAll(from: Map<out K, V>) = withLock { delegate.putAll(from) }
    actual override fun remove(key: K) = withLock { delegate.remove(key) }
    actual override fun containsKey(key: K) = withLock { delegate.containsKey(key) }
    actual override fun containsValue(value: V) = withLock { delegate.containsValue(value) }
    actual override operator fun get(key: K) = withLock { delegate[key] }
    actual override fun isEmpty() = withLock { delegate.isEmpty() }
}
