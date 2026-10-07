package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import kotlinx.serialization.Serializable

/**
 * Create file operation
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#createFile)
 */
@Serializable
data class CreateFile(
    /**
     * The resource to create.
     */
    val uri: DocumentUri,

    /**
     * Additional options.
     */
    var options: CreateFileOptions? = null,
    override var annotationId: ChangeAnnotationIdentifier? = null
) : ResourceOperation {
    /**
     * This is a create operation.
     */
    override val kind = ResourceOperationKind.Create
}

/**
 * Options to create a file.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#createFileOptions)
 */
@Serializable
data class CreateFileOptions(
    /**
     * Overwrite existing file. Overwrite wins over [ignoreIfExists].
     */
    var overwrite: Boolean? = null,

    /**
     * Ignore if exists.
     */
    var ignoreIfExists: Boolean? = null
)
