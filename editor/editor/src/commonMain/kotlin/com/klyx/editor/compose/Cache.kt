package com.klyx.editor.compose

import androidx.collection.LruCache
import androidx.collection.lruCache
import androidx.compose.ui.text.TextLayoutResult

internal val TextLineCache = lruCache<String, TextLayoutResult>(100)
internal val LineNumberCache = lruCache<String, TextLayoutResult>(100)
internal val LineWidthCache = lruCache<Int, Int>(50)

internal fun invalidateCache() {
    TextLineCache.evictAll()
    LineNumberCache.evictAll()
    LineWidthCache.evictAll()
}

internal inline fun <K : Any, V : Any> LruCache<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        put(key, answer)
        answer
    } else {
        value
    }
}
