package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#typeDefinitionOptions)
 */
@Serializable
open class TypeDefinitionOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#typeDefinitionRegistrationOptions)
 */
@Serializable
data class TypeDefinitionRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : TypeDefinitionOptions(), TextDocumentRegistrationOptions, StaticRegistrationOptions
