package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Inline completion options used during static registration.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineCompletionOptions)
 *
 * @since 3.18.0
 */
@Serializable
open class InlineCompletionOptions : WorkDoneProgressOptions()

/**
 * Inline completion options used during static or dynamic registration.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineCompletionRegistrationOptions)
 *
 * @since 3.18.0
 */
@Serializable
data class InlineCompletionRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : TextDocumentRegistrationOptions, StaticRegistrationOptions, InlineCompletionOptions()
