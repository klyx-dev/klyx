package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Client capabilities for the show document request.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#window_showDocument)
 *
 * @since 3.16.0
 */
@Serializable
data class ShowDocumentCapabilities(
    /**
     * The client has support for the show document
     * request.
     */
    val support: Boolean
)
