package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentRangeFormattingClientCapabilities)
 */
@Serializable
data class RangeFormattingCapabilities(
    /**
     * Whether formatting supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Whether the client supports formatting multiple ranges at once.
     *
     * @since 3.18.0
     * @proposed
     */
    var rangesSupport: Boolean? = null
) : DynamicRegistrationCapabilities
