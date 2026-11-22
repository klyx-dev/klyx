package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Client capabilities specific to inline values.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineValueClientCapabilities)
 *
 * @since 3.17.0
 */
@Serializable
data class InlineValueClientCapabilities(
    /**
     * Whether the implementation supports dynamic registration for inline
     * value providers.
     */
    override var dynamicRegistration: Boolean? = null
) : DynamicRegistrationCapabilities
