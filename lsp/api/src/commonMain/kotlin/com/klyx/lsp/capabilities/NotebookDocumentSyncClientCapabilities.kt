package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Notebook specific client capabilities.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookDocumentSyncClientCapabilities)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookDocumentSyncClientCapabilities(
    /**
     * Whether implementation supports dynamic registration. If this is
     * set to `true`, the client supports the new
     * `(NotebookDocumentSyncRegistrationOptions & NotebookDocumentSyncOptions)`
     * return value for the corresponding server capability as well.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * The client supports sending execution summary data per cell.
     */
    var executionSummarySupport: Boolean? = null
) : DynamicRegistrationCapabilities
