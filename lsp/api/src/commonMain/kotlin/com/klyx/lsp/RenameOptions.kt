package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#renameOptions)
 */
@Serializable
open class RenameOptions(
    /**
     * Renames should be checked and tested before being executed.
     */
    var prepareProvider: Boolean? = null
) : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#renameRegistrationOptions)
 */
@Serializable
data class RenameRegistrationOptions(
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, RenameOptions()
