package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Capabilities specific to the notebook document support.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookDocumentClientCapabilities)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookDocumentClientCapabilities(
    /**
     * Capabilities specific to notebook document synchronization
     *
     * @since 3.17.0
     */
    var synchronization: NotebookDocumentSyncCapabilities? = null
)
