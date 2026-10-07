package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#definitionOptions)
 */
@Serializable
open class DefinitionOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#definitionRegistrationOptions)
 */
@Serializable
data class DefinitionRegistrationOptions(
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, DefinitionOptions()
