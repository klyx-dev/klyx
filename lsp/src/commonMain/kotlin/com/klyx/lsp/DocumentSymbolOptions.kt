package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentSymbolOptions)
 */
@Serializable
open class DocumentSymbolOptions(
    /**
     * A human-readable string that is shown when multiple outlines trees
     * are shown for the same document.
     *
     * @since 3.16.0
     */
    var label: String? = null,
) : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentSymbolRegistrationOptions)
 */
@Serializable
data class DocumentSymbolRegistrationOptions(
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, DocumentSymbolOptions()
