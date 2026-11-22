package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Options specific to a notebook plus its cells
 * to be synced to the server.
 *
 * If a selector provides a notebook document
 * filter but no cell selector, all cells of a
 * matching notebook document will be synced.
 *
 * If a selector provides no notebook document
 * filter but only a cell selector, all notebook
 * documents that contain at least one matching
 * cell will be synced.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookDocumentSyncOptions)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookDocumentSyncOptions(
    /**
     * The notebooks to be synced
     */
    val notebookSelector: List<NotebookSelector>,

    /**
     * Whether save notifications should be forwarded to
     * the server. Will only be honored if mode === `notebook`.
     */
    var save: Boolean? = null
)

/**
 * Registration options specific to a notebook.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#notebookDocumentSyncRegistrationOptions)
 *
 * @since 3.17.0
 */
@Serializable
data class NotebookDocumentSyncRegistrationOptions(
    /**
     * The notebooks to be synced
     */
    val notebookSelector: List<NotebookSelector>,

    /**
     * Whether save notifications should be forwarded to
     * the server. Will only be honored if mode === `notebook`.
     */
    var save: Boolean? = null,
    override var id: String? = null,
) : StaticRegistrationOptions
