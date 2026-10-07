package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#linkedEditingRangeOptions)
 */
@Serializable
open class LinkedEditingRangeOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#linkedEditingRangeRegistrationOptions)
 */
@Serializable
data class LinkedEditingRangeRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override val id: String? = null
) : TextDocumentRegistrationOptions, LinkedEditingRangeOptions(), StaticRegistrationOptions
