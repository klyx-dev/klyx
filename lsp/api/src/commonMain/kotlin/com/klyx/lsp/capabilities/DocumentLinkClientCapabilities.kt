package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentLinkClientCapabilities)
 */
@Serializable
data class DocumentLinkClientCapabilities(
    /**
     * Whether document link supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Whether the client supports the `tooltip` property on `DocumentLink`.
     *
     * @since 3.15.0
     */
    var tooltipSupport: Boolean? = null
) : DynamicRegistrationCapabilities
