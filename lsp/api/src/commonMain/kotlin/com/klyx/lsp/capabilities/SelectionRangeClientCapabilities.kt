package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#selectionRangeClientCapabilities)
 */
@Serializable
data class SelectionRangeClientCapabilities(
    /**
     * Whether the implementation supports dynamic registration for selection range
     * providers. If this is set to `true`, the client supports the new
     * `SelectionRangeRegistrationOptions` return value for the corresponding
     * server capability as well.
     */
    override val dynamicRegistration: Boolean? = null
) : DynamicRegistrationCapabilities
