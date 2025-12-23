package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Show message request client capabilities
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#window_showMessageRequest)
 */
@Serializable
data class ShowMessageRequestClientCapabilities(
    /**
     * Capabilities specific to the `MessageActionItem` type.
     */
    var messageActionItem: MessageActionItemClientCapabilities? = null
)

/**
 * Capabilities specific to the `MessageActionItem` type.
 */
@Serializable
data class MessageActionItemClientCapabilities(
    /**
     * Whether the client supports additional attributes which
     * are preserved and sent back to the server in the
     * request's response.
     */
    var additionalPropertiesSupport: Boolean? = null
)
