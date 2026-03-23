package com.klyx.util

import java.util.concurrent.ConcurrentHashMap

actual typealias ConcurrentMap<K, V> = ConcurrentHashMap<K & Any, V & Any>
