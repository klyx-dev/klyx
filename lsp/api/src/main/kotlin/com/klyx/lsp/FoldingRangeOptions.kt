package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#foldingRangeOptions)
 */
@Serializable
open class FoldingRangeOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#foldingRangeRegistrationOptions)
 */
@Serializable
data class FoldingRangeRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : TextDocumentRegistrationOptions, StaticRegistrationOptions, FoldingRangeOptions()
