package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import kotlinx.serialization.Serializable

/**
 * Rename file operation
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#renameFile)
 */
@Serializable
data class RenameFile(
    /**
     * The old (existing) location.
     */
    val oldUri: DocumentUri,

    /**
     * The new location.
     */
    val newUri: DocumentUri,

    /**
     * Rename options.
     */
    var options: RenameFileOptions? = null,
    override var annotationId: ChangeAnnotationIdentifier? = null
) : ResourceOperation {
    /**
     * This is a rename operation.
     */
    override val kind = ResourceOperationKind.Rename
}

/**
 * Rename file options
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#renameFileOptions)
 */
@Serializable
data class RenameFileOptions(
    /**
     * Overwrite existing file. Overwrite wins over [ignoreIfExists].
     */
    var overwrite: Boolean? = null,

    /**
     * Ignore if exists.
     */
    var ignoreIfExists: Boolean? = null
)
