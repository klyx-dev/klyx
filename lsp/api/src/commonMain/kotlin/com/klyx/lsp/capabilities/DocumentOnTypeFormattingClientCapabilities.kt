package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentOnTypeFormattingClientCapabilities)
 */
@Serializable
data class DocumentOnTypeFormattingClientCapabilities(
    /**
     * Whether on type formatting supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null
) : DynamicRegistrationCapabilities
