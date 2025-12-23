package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Additional data about a workspace edit.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceEditMetadata)
 *
 * @since 3.18.0
 * @proposed
 */
@Serializable
data class WorkspaceEditMetadata(
    /**
     * Signal to the editor that this edit is a refactoring.
     */
    var isRefactoring: Boolean? = null
)
