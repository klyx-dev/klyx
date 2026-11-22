package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Represents information on a file/folder delete.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileDelete)
 *
 * @since 3.16.0
 */
@Serializable
data class FileDelete(
    /**
     * A file:// URI for the location of the file/folder being deleted.
     */
    val uri: String
)
