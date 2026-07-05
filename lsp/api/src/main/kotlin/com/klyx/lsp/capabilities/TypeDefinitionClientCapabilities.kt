package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#typeDefinitionClientCapabilities)
 */
@Serializable
data class TypeDefinitionClientCapabilities(
    /**
     * Whether implementation supports dynamic registration. If this is set to
     * `true`, the client supports the new `TypeDefinitionRegistrationOptions`
     * return value for the corresponding server capability as well.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * The client supports additional metadata in the form of definition links.
     *
     * @since 3.14.0
     */
    var linkSupport: Boolean? = null
) : DynamicRegistrationCapabilities
