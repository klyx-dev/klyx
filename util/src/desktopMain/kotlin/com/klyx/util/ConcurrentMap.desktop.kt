package com.klyx.util

import java.util.concurrent.ConcurrentHashMap

actual class ConcurrentMap<K, V> actual constructor() : MutableMap<K, V> {
    @Suppress("JavaCollectionWithNullableTypeArgument")
    private val delegate = ConcurrentHashMap<K, V>()

    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = delegate.entries
    actual override val keys: MutableSet<K> get() = delegate.keys
    actual override val values: MutableCollection<V> get() = delegate.values
    actual override val size: Int get() = delegate.size

    actual override fun clear() = delegate.clear()
    actual override fun put(key: K, value: V) = delegate.put(key, value)
    actual override fun putAll(from: Map<out K, V>) = delegate.putAll(from)
    actual override fun remove(key: K) = delegate.remove(key)
    actual override fun containsKey(key: K) = delegate.containsKey(key)
    actual override fun containsValue(value: V) = delegate.containsValue(value)
    actual override operator fun get(key: K) = delegate[key]
    actual override fun isEmpty() = delegate.isEmpty()
}
