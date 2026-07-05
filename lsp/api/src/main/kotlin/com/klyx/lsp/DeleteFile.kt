package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import kotlinx.serialization.Serializable

/**
 * Delete file operation
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#deleteFile)
 */
@Serializable
data class DeleteFile(
    /**
     * The file to delete.
     */
    val uri: DocumentUri,

    /**
     * Delete options.
     */
    var options: DeleteFileOptions? = null,
    override val annotationId: ChangeAnnotationIdentifier? = null
) : ResourceOperation {
    /**
     * This is a delete operation.
     */
    override val kind = ResourceOperationKind.Delete
}

/**
 * Delete file options
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#deleteFileOptions)
 */
@Serializable
data class DeleteFileOptions(
    /**
     * Delete the content recursively if a folder is denoted.
     */
    val recursive: Boolean? = null,

    /**
     * Ignore the operation if the file doesn't exist.
     */
    val ignoreIfNotExists: Boolean? = null
)
