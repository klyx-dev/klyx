package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentColorClientCapabilities)
 */
@Serializable
data class DocumentColorClientCapabilities(
    /**
     * Whether document color supports dynamic registration.
     */
    override val dynamicRegistration: Boolean? = null
) : DynamicRegistrationCapabilities
