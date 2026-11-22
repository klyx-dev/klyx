package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentHighlightOptions)
 */
@Serializable
open class DocumentHighlightOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentHighlightRegistrationOptions)
 */
@Serializable
data class DocumentHighlightRegistrationOptions(
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, DocumentHighlightOptions()
