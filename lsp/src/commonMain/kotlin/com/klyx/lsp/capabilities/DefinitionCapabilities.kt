package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#definitionClientCapabilities)
 */
@Serializable
data class DefinitionCapabilities(
    /**
     * Whether definition supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,


    /**
     * The client supports additional metadata in the form of definition links.
     *
     * @since 3.14.0
     */
    var linkSupport: Boolean? = null
) : DynamicRegistrationCapabilities
