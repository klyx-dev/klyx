package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Represents information on a file/folder rename.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileRename)
 *
 * @since 3.16.0
 */
@Serializable
data class FileRename(
    /**
     * A file:// URI for the original location of the file/folder being renamed.
     */
    val oldUri: String,

    /**
     * A file:// URI for the new location of the file/folder being renamed.
     */
    val newUri: String
)
