package com.klyx.core.util

fun <K, V> Map<K, V>.asHashMap(): HashMap<K, V> = HashMap(this)
