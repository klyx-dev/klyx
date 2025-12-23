package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentColorOptions)
 */
@Serializable
open class DocumentColorOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentColorRegistrationOptions)
 */
@Serializable
data class DocumentColorRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : TextDocumentRegistrationOptions, StaticRegistrationOptions, DocumentColorOptions()
