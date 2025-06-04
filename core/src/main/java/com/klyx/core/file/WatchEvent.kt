package com.klyx.core.file

import java.io.File

// https://github.com/vishna/watchservice-ktx

/**
 * Wrapper around [WatchEvent] that comes with properly resolved absolute path
 */
data class KWatchEvent(
    /**
     * Abolute path of modified folder/file
     */
    val file: File,

    /**
     * Kind of file system event
     */
    val kind: Kind,

    /**
     * Optional extra data that should be associated with this event
     */
    val tag: Any?
) {
    /**
     * File system event, wrapper around [WatchEvent.Kind]
     */
    enum class Kind(val kind: String) {
        /**
         * Triggered upon initialization of the channel
         */
        Initialized("initialized"),

        /**
         * Triggered when file or directory is created
         */
        Created("created"),

        /**
         * Triggered when file or directory is modified
         */
        Modified("modified"),

        /**
         * Triggered when file or directory is deleted
         */
        Deleted("deleted")
    }
}
