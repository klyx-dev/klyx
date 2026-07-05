package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#declarationClientCapabilities)
 */
@Serializable
data class DeclarationClientCapabilities(
    /**
     * Whether declaration supports dynamic registration. If this is set to
     * `true`, the client supports the new `DeclarationRegistrationOptions`
     * return value for the corresponding server capability as well.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * The client supports additional metadata in the form of declaration links.
     */
    var linkSupport: Boolean? = null
) : DynamicRegistrationCapabilities
