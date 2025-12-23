package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#declarationOptions)
 */
@Serializable
open class DeclarationOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#declarationRegistrationOptions)
 */
@Serializable
data class DeclarationRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override val id: String? = null
) : DeclarationOptions(), TextDocumentRegistrationOptions, StaticRegistrationOptions
