package com.klyx.lsp.capabilities

import com.klyx.lsp.FailureHandlingKind
import com.klyx.lsp.ResourceOperationKind
import com.klyx.lsp.WorkspaceEdit
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceEditClientCapabilities)
 */
@Serializable
data class WorkspaceEditCapabilities(
    /**
     * The client supports versioned document changes in [WorkspaceEdit]s.
     */
    var documentChanges: Boolean? = null,

    /**
     * The resource operations the client supports. Clients should at least
     * support 'create', 'rename', and 'delete' for files and folders.
     *
     * @since 3.13.0
     */
    var resourceOperations: List<ResourceOperationKind>? = null,

    /**
     * The failure handling strategy of a client if applying the workspace edit
     * fails.
     *
     * @since 3.13.0
     */
    var failureHandling: FailureHandlingKind? = null,

    /**
     * Whether the client normalizes line endings to the client specific
     * setting.
     * If set to `true`, the client will normalize line ending characters
     * in a workspace edit to the client specific new line character(s).
     *
     * @since 3.16.0
     */
    var normalizesLineEndings: Boolean? = null,

    /**
     * Whether the client in general supports change annotations on text edits,
     * create file, rename file, and delete file changes.
     *
     * @since 3.16.0
     */
    var changeAnnotationSupport: ChangeAnnotationSupportCapabilities? = null,

    /**
     * Whether the client supports `WorkspaceEditMetadata` in `WorkspaceEdit`s.
     *
     * @since 3.18.0
     * @proposed
     */
    var metadataSupport: Boolean? = null,

    /**
     * Whether the client supports snippets as text edits.
     *
     * @since 3.18.0
     * @proposed
     */
    var snippetEditSupport: Boolean? = null,
)

/**
 * Whether the client in general supports change annotations on text edits,
 * create file, rename file, and delete file changes.
 *
 * @since 3.16.0
 */
@Serializable
data class ChangeAnnotationSupportCapabilities(
    /**
     * Whether the client groups edits with equal labels into tree nodes,
     * for instance all edits labelled with "Changes in Strings" would
     * be a tree node.
     */
    var groupsOnLabel: Boolean? = null
)
