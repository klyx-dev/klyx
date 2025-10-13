package com.klyx.core.event

fun interface Subscriber<E : Any> {
    suspend fun onEvent(event: E)
}
