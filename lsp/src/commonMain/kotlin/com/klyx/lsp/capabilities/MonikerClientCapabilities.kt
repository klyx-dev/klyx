package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#monikerClientCapabilities)
 */
@Serializable
data class MonikerClientCapabilities(
    /**
     * Whether implementation supports dynamic registration. If this is set to
     * `true`, the client supports the new `(TextDocumentRegistrationOptions &
     * StaticRegistrationOptions)` return value for the corresponding server
     * capability as well.
     */
    override var dynamicRegistration: Boolean? = null
) : DynamicRegistrationCapabilities
