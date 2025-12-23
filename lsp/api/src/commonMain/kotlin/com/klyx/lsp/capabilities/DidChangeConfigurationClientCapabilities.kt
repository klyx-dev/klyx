package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didChangeConfigurationClientCapabilities)
 */
@Serializable
data class DidChangeConfigurationClientCapabilities(
    /**
     * Did change configuration notification supports dynamic registration.
     *
     * @since 3.6.0 to support the new pull model.
     */
    override var dynamicRegistration: Boolean? = null
) : DynamicRegistrationCapabilities

