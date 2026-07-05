package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * The workspace folder change event.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceFoldersChangeEvent)
 */
@Serializable
data class WorkspaceFoldersChangeEvent(
    /**
     * The array of added workspace folders.
     */
    val added: List<WorkspaceFolder>,

    /**
     * The array of removed workspace folders.
     */
    val removed: List<WorkspaceFolder>,
)
