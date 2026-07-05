package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#signatureHelpOptions)
 */
@Serializable
data class SignatureHelpOptions(
    /**
     * The characters that trigger signature help
     * automatically.
     */
    val triggerCharacters: List<String>?,

    /**
     * List of characters that re-trigger signature help.
     *
     * These trigger characters are only active when signature help is already
     * showing. All trigger characters are also counted as re-trigger
     * characters.
     *
     * @since 3.15.0
     */
    val retriggerCharacters: List<String>?
) : WorkDoneProgressOptions()

@Serializable
data class SignatureHelpRegistrationOptions(
    /**
     * The characters that trigger signature help
     * automatically.
     */
    val triggerCharacters: List<String>?,

    /**
     * List of characters that re-trigger signature help.
     *
     * These trigger characters are only active when signature help is already
     * showing. All trigger characters are also counted as re-trigger
     * characters.
     *
     * @since 3.15.0
     */
    val retriggerCharacters: List<String>?,
    override var documentSelector: DocumentSelector? = null
) : TextDocumentRegistrationOptions, WorkDoneProgressOptions()
