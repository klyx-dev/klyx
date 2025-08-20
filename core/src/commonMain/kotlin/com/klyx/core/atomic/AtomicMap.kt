package com.klyx.core.atomic

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

class AtomicMap<K, V>(
    initial: Map<K, V>
) : MutableMap<K, V> {
    private val _map = atomic(initial.toMutableMap())
    private val map by _map

    constructor() : this(mapOf())

    override val keys = map.keys
    override val values = map.values
    override val entries = map.entries

    override fun put(key: K, value: V) = run {
        val prevValue = map[key]
        _map.update { it.apply { put(key, value) } }
        prevValue
    }

    override fun remove(key: K): V? = run {
        val prevValue = map[key]
        _map.update { it.apply { remove(key) } }
        prevValue
    }

    override fun putAll(from: Map<out K, V>) {
        _map.update { it.apply { putAll(from) } }
    }

    override fun clear() {
        _map.update { mutableMapOf() }
    }

    override val size get() = map.size
    override fun isEmpty() = map.isEmpty()
    override fun containsKey(key: K) = map.containsKey(key)
    override fun containsValue(value: V) = map.containsValue(value)
    override fun get(key: K) = map[key]
}

fun <K, V> atomicMapOf(): AtomicMap<K, V> = AtomicMap()

fun <K, V> atomicMapOf(vararg pairs: Pair<K, V>): AtomicMap<K, V> = AtomicMap(pairs.toMap())
