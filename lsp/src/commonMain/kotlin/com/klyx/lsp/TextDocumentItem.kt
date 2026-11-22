package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentItem)
 */
@Serializable
data class TextDocumentItem(
    /**
     * The text document's URI.
     */
    val uri: DocumentUri,

    /**
     * The text document's language identifier.
     */
    val languageId: String,

    /**
     * The version number of this document (it will increase after each
     * change, including undo/redo).
     */
    val version: Int,

    /**
     * The content of the opened text document.
     */
    val text: String
)
