package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentOnTypeFormattingOptions)
 */
@Serializable
data class DocumentOnTypeFormattingOptions(
    /**
     * A character on which formatting should be triggered, like `{`.
     */
    val firstTriggerCharacter: String,

    /**
     * More trigger characters.
     */
    var moreTriggerCharacter: List<String>? = null
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentOnTypeFormattingRegistrationOptions)
 */
@Serializable
data class DocumentOnTypeFormattingRegistrationOptions(
    /**
     * A character on which formatting should be triggered, like `{`.
     */
    val firstTriggerCharacter: String,

    /**
     * More trigger characters.
     */
    var moreTriggerCharacter: List<String>? = null,
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions

