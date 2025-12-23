package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#clientCapabilities)
 *
 * @property workspace Workspace specific client capabilities.
 * @property textDocument Text document specific client capabilities.
 * @property notebookDocument Capabilities specific to the notebook document support.
 * @property window Window specific client capabilities.
 * @property general General client capabilities.
 * @property experimental Experimental client capabilities.
 */
@Serializable
data class ClientCapabilities(
    /**
     * Workspace specific client capabilities.
     */
    var workspace: WorkspaceClientCapabilities? = null,

    /**
     * Text document specific client capabilities.
     */
    var textDocument: TextDocumentClientCapabilities? = null,

    /**
     * Capabilities specific to the notebook document support.
     *
     * @since 3.17.0
     */
    var notebookDocument: NotebookDocumentClientCapabilities? = null,

    /**
     * Window specific client capabilities.
     */
    var window: WindowClientCapabilities? = null,

    /**
     * General client capabilities.
     *
     * @since 3.16.0
     */
    var general: GeneralClientCapabilities? = null,

    /**
     * Experimental client capabilities.
     */
    var experimental: JsonElement? = null
)

