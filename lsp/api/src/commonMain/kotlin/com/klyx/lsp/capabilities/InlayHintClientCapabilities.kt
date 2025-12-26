package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Inlay hint client capabilities.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlayHintClientCapabilities)
 *
 * @since 3.17.0
 */
@Serializable
data class InlayHintClientCapabilities(
    /**
     * Whether inlay hints support dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Indicates which properties a client can resolve lazily on an inlay
     * hint.
     */
    var resolveSupport: ResolveSupportCapabilities? = null
) : DynamicRegistrationCapabilities
