package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Client capabilities specific to inline completions.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineCompletionClientCapabilities)
 *
 * @since 3.18.0
 */
@Serializable
data class InlineCompletionClientCapabilities(
    /**
     * Whether implementation supports dynamic registration for inline
     * completion providers.
     */
    override var dynamicRegistration: Boolean? = null
) : DynamicRegistrationCapabilities
