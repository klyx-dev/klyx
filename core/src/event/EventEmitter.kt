package com.klyx.core.event

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A per owner broadcast channel for "this happened" notifications.
 */
class EventEmitter<E : Any>(bufferCapacity: Int = 1024) {
    private val shared = MutableSharedFlow<E>(
        replay = 0,
        extraBufferCapacity = bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<E> = shared.asSharedFlow()

    fun emit(event: E) {
        shared.tryEmit(event)
    }
}
