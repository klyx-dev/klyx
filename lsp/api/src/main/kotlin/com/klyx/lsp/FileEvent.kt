package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * An event describing a file change.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileEvent)
 */
@Serializable
data class FileEvent(
    /**
     * The file's URI.
     */
    val uri: DocumentUri,

    /**
     * The change type.
     */
    val type: FileChangeType
)

/**
 * The file event type.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileChangeType)
 */
@JvmInline
@Serializable
value class FileChangeType private constructor(private val value: Int) {
    companion object {
        /**
         * The file got created.
         */
        val Created = FileChangeType(1)

        /**
         * The file got changed.
         */
        val Changed = FileChangeType(2)

        /**
         * The file got deleted.
         */
        val Deleted = FileChangeType(3)
    }
}
