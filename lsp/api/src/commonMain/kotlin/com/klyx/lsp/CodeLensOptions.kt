package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeLensOptions)
 */
@Serializable
open class CodeLensOptions(
    /**
     * Code lens has a resolve provider as well.
     */
    var resolveProvider: Boolean? = null
) : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeLensRegistrationOptions)
 */
@Serializable
data class CodeLensRegistrationOptions(
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, CodeLensOptions()
