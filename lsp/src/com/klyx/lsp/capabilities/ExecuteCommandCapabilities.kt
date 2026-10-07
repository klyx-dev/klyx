package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#executeCommandClientCapabilities)
 */
@Serializable
data class ExecuteCommandCapabilities(
    /**
     * Execute command supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null
) : DynamicRegistrationCapabilities
