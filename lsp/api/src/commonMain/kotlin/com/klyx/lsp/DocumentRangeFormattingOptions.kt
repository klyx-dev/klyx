package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentRangeFormattingOptions)
 */
@Serializable
open class DocumentRangeFormattingOptions(
    /**
     * Whether the server supports formatting multiple ranges at once.
     *
     * @since 3.18.0
     * @proposed
     */
    var rangesSupport: Boolean? = null
) : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentRangeFormattingRegistrationOptions)
 */
@Serializable
data class DocumentRangeFormattingRegistrationOptions(
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, DocumentRangeFormattingOptions()
