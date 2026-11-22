package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentLinkOptions)
 */
@Serializable
open class DocumentLinkOptions(
    /**
     * Document links have a resolve provider as well.
     */
    var resolveProvider: Boolean? = null
) : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentLinkRegistrationOptions)
 */
@Serializable
data class DocumentLinkRegistrationOptions(
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, DocumentLinkOptions()
