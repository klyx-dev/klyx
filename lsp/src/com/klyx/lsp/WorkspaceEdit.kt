package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceEdit)
 */
@Serializable
data class WorkspaceEdit(
    /**
     * Holds changes to existing resources.
     */
    var changes: Map<DocumentUri, List<TextEdit>>? = null,

    /**
     * Depending on the client capability
     * `workspace.workspaceEdit.resourceOperations` document changes are either
     * an array of [TextDocumentEdit]s to express changes to n different text
     * documents where each text document edit addresses a specific version of
     * a text document. Or it can contain above [TextDocumentEdit]s mixed with
     * create, rename and delete file / folder operations.
     *
     * Whether a client supports versioned document edits is expressed via
     * `workspace.workspaceEdit.documentChanges` client capability.
     *
     * If a client neither supports `documentChanges` nor
     * `workspace.workspaceEdit.resourceOperations` then only plain [TextEdit]s
     * using the [changes] property are supported.
     */
    var documentChanges: List<OneOf<TextDocumentEdit, ResourceOperation>>? = null,

    /**
     * A map of change annotations that can be referenced in
     * [AnnotatedTextEdit]s or create, rename and delete file / folder
     * operations.
     *
     * Whether clients honor this property depends on the client capability
     * `workspace.changeAnnotationSupport`.
     *
     * @since 3.16.0
     */
    var changeAnnotations: Map<ChangeAnnotationIdentifier, ChangeAnnotation>? = null
)
