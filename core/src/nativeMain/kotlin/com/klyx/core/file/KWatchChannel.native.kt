package com.klyx.core.file

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelIterator
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.selects.SelectClause2

@OptIn(markerClass = [DelicateCoroutinesApi::class])
actual open class KWatchChannel actual constructor(
    file: KxFile,
    scope: CoroutineScope,
    mode: Mode,
    tag: Any?,
    channel: Channel<KWatchEvent>
) : Channel<KWatchEvent> by channel {
    actual val file: KxFile
        get() = TODO("Not yet implemented")
    actual val scope: CoroutineScope
        get() = TODO("Not yet implemented")
    actual val mode: Mode
        get() = TODO("Not yet implemented")
    actual val tag: Any?
        get() = TODO("Not yet implemented")

    @DelicateCoroutinesApi
    actual override val isClosedForSend: Boolean
        get() = TODO("Not yet implemented")
    actual override val onSend: SelectClause2<KWatchEvent, SendChannel<KWatchEvent>>
        get() = TODO("Not yet implemented")

    actual override suspend fun send(element: KWatchEvent) {
    }

    actual override fun trySend(element: KWatchEvent): ChannelResult<Unit> {
        TODO("Not yet implemented")
    }

    actual override fun close(cause: Throwable?): Boolean {
        TODO("Not yet implemented")
    }

    actual override fun invokeOnClose(handler: (Throwable?) -> Unit) {
    }

    @DelicateCoroutinesApi
    actual override val isClosedForReceive: Boolean
        get() = TODO("Not yet implemented")

    @ExperimentalCoroutinesApi
    actual override val isEmpty: Boolean
        get() = TODO("Not yet implemented")
    actual override val onReceive: SelectClause1<KWatchEvent>
        get() = TODO("Not yet implemented")
    actual override val onReceiveCatching: SelectClause1<ChannelResult<KWatchEvent>>
        get() = TODO("Not yet implemented")

    actual override suspend fun receive(): KWatchEvent {
        TODO("Not yet implemented")
    }

    actual override suspend fun receiveCatching(): ChannelResult<KWatchEvent> {
        TODO("Not yet implemented")
    }

    actual override fun tryReceive(): ChannelResult<KWatchEvent> {
        TODO("Not yet implemented")
    }

    actual override operator fun iterator(): ChannelIterator<KWatchEvent> {
        TODO("Not yet implemented")
    }

    actual override fun cancel(cause: CancellationException?) {
    }

    actual override fun cancel(cause: Throwable?): Boolean {
        TODO("Not yet implemented")
    }
}

@OptIn(markerClass = [DelicateCoroutinesApi::class])
actual fun KWatchChannel(
    file: KxFile,
    mode: Mode,
    scope: CoroutineScope,
    tag: Any?
) = KWatchChannel(file, scope, mode, tag)
