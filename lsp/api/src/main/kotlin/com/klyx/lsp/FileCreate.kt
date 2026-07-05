package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Represents information on a file/folder create.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileCreate)
 *
 * @since 3.16.0
 */
@Serializable
data class FileCreate(
    /**
     * A file:// URI for the location of the file/folder being created.
     */
    val uri: String
)
