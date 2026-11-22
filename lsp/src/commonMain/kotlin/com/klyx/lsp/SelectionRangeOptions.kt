package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#selectionRangeOptions)
 */
@Serializable
open class SelectionRangeOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#selectionRangeRegistrationOptions)
 */
@Serializable
data class SelectionRangeRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : SelectionRangeOptions(), TextDocumentRegistrationOptions, StaticRegistrationOptions
