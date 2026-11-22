package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#hoverOptions)
 */
@Serializable
open class HoverOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#hoverRegistrationOptions)
 */
data class HoverRegistrationOptions(
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, HoverOptions()
