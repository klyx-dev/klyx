package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentHighlightClientCapabilities)
 */
@Serializable
data class DocumentHighlightClientCapabilities(
    /**
     * Whether document highlight supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null
) : DynamicRegistrationCapabilities
