package com.klyx.lsp.capabilities

import com.klyx.lsp.MarkupKind
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#hoverClientCapabilities)
 */
@Serializable
data class HoverCapabilities(
    /**
     * Whether hover supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Client supports the following content formats if the content
     * property refers to a `literal of type MarkupContent`.
     * The order describes the preferred format of the client.
     */
    var contentFormat: List<MarkupKind>? = null
) : DynamicRegistrationCapabilities
