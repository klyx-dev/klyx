package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * The result of a show document request.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#showDocumentResult)
 *
 * @since 3.16.0
 */
@Serializable
data class ShowDocumentResult(
    /**
     * A boolean indicating if the show was successful.
     */
    val success: Boolean
)
