package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Result of the `workspace/textDocumentContent` request.
 *
 * @since 3.18.0
 * @proposed
 */
@Serializable
data class TextDocumentContentResult(
    /**
     * The text content of the text document. Please note, that the content of
     * any subsequent open notifications for the text document might differ
     * from the returned content due to whitespace and line ending
     * normalizations done on the client
     */
    val text: String
)
