package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#textDocumentPositionParams)
 */
@Serializable
sealed interface TextDocumentPositionParams {
    /**
     * The text document.
     */
    val textDocument: TextDocumentIdentifier

    /**
     * The position inside the text document.
     */
    val position: Position
}
