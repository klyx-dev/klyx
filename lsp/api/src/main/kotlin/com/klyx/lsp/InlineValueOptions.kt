package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Inline value options used during static registration.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineValueOptions)
 *
 * @since 3.17.0
 */
@Serializable
open class InlineValueOptions : WorkDoneProgressOptions()

/**
 * Inline value options used during static or dynamic registration.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineValueRegistrationOptions)
 *
 * @since 3.17.0
 */
@Serializable
data class InlineValueRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : InlineValueOptions(), TextDocumentRegistrationOptions, StaticRegistrationOptions

