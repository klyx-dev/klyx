package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#monikerOptions)
 */
@Serializable
open class MonikerOptions : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#monikerRegistrationOptions)
 */
@Serializable
data class MonikerRegistrationOptions(
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, MonikerOptions()
