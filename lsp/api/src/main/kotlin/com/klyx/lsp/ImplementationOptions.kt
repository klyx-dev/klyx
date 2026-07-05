package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#implementationOptions)
 */
@Serializable
open class ImplementationOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#implementationRegistrationOptions)
 */
@Serializable
data class ImplementationRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : TextDocumentRegistrationOptions, ImplementationOptions(), StaticRegistrationOptions
