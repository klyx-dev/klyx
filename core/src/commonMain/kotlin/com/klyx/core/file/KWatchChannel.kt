package com.klyx.core.file

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel

// https://github.com/vishna/watchservice-ktx

/**
 * Channel based wrapper for Java's WatchService
 *
 * @param [file] - file or directory that is supposed to be monitored by WatchService
 * @param [scope] - CoroutineScope in within which Channel's sending loop will be running
 * @param [mode] - channel can work in one of the three modes: watching a single file,
 * watching a single directory or watching directory tree recursively
 * @param [tag] - any kind of data that should be associated with this channel, optional
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(DelicateCoroutinesApi::class)
expect class KWatchChannel(
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
