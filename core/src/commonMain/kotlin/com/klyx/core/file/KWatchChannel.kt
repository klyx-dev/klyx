package com.klyx.core.file

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelIterator
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.selects.SelectClause2

/**
 * Channel based wrapper for Java's WatchService
 *
 * @param [file] - file or directory that is supposed to be monitored by WatchService
 * @param [scope] - CoroutineScope in within which Channel's sending loop will be running
 * @param [mode] - channel can work in one of the three modes: watching a single file,
 * watching a single directory or watching directory tree recursively
 * @param [tag] - any kind of data that should be associated with this channel, optional
 */
@OptIn(DelicateCoroutinesApi::class)
expect open class KWatchChannel(
    file: KxFile,
    scope: CoroutineScope = GlobalScope,
    mode: Mode,
    tag: Any? = null,
    channel: Channel<KWatchEvent> = Channel()
) : Channel<KWatchEvent> {
    val file: KxFile
    val scope: CoroutineScope
    val mode: Mode
    val tag: Any?

    @DelicateCoroutinesApi
    override val isClosedForSend: Boolean
    override val onSend: SelectClause2<KWatchEvent, SendChannel<KWatchEvent>>
    override suspend fun send(element: KWatchEvent)
    override fun trySend(element: KWatchEvent): ChannelResult<Unit>
    override fun close(cause: Throwable?): Boolean
    override fun invokeOnClose(handler: (Throwable?) -> Unit)

    @DelicateCoroutinesApi
    override val isClosedForReceive: Boolean

    @ExperimentalCoroutinesApi
    override val isEmpty: Boolean
    override val onReceive: SelectClause1<KWatchEvent>
    override val onReceiveCatching: SelectClause1<ChannelResult<KWatchEvent>>
    override suspend fun receive(): KWatchEvent
    override suspend fun receiveCatching(): ChannelResult<KWatchEvent>
    override fun tryReceive(): ChannelResult<KWatchEvent>
    override fun iterator(): ChannelIterator<KWatchEvent>
    override fun cancel(cause: CancellationException?)
    override fun cancel(cause: Throwable?): Boolean
}

/**
 * Describes the mode this channels is running in
 */
enum class Mode {
    /**
     * Watches only the given file
     */
    SingleFile,

    /**
     * Watches changes in the given directory, changes in subdirectories will be
     * ignored
     */
    SingleDirectory,

    /**
     * Watches changes in subdirectories
     */
    Recursive
}

/**
 * Watches directory. If file is supplied it will use parent directory. If it's an intent to watch just file,
 * developers must filter for the file related events themselves.
 *
 * From: [watchservice-ktx](https://github.com/vishna/watchservice-ktx)
 *
 * @param [mode] - mode in which we should observe changes, can be SingleFile, SingleDirectory, Recursive
 * @param [tag] - any kind of data that should be associated with this channel
 * @param [scope] - coroutine context for the channel, optional
 */
@OptIn(DelicateCoroutinesApi::class)
fun KxFile.asWatchChannel(
    mode: Mode? = null,
    tag: Any? = null,
    scope: CoroutineScope = GlobalScope
) = KWatchChannel(
    file = this,
    mode = mode ?: if (isFile) Mode.SingleFile else Mode.Recursive,
    scope = scope,
    tag = tag
)

@OptIn(DelicateCoroutinesApi::class)
expect fun KWatchChannel(
    file: KxFile,
    mode: Mode,
    scope: CoroutineScope = GlobalScope,
    tag: Any? = null
): KWatchChannel
