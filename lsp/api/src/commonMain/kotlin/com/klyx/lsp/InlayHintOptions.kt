package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Inlay hint options used during static registration.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlayHintOptions)
 *
 * @since 3.17.0
 */
@Serializable
open class InlayHintOptions(
    /**
     * The server provides support to resolve additional
     * information for an inlay hint item.
     */
    var resolveProvider: Boolean? = null
) : WorkDoneProgressOptions()

/**
 * Inlay hint options used during static or dynamic registration.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlayHintRegistrationOptions)
 *
 * @since 3.17.0
 */
@Serializable
data class InlayHintRegistrationOptions(
    override var documentSelector: DocumentSelector? = null,
    override var id: String? = null
) : InlayHintOptions(), TextDocumentRegistrationOptions, StaticRegistrationOptions
