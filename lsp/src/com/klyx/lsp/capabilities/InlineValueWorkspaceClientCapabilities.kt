package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Client workspace capabilities specific to inline values.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#inlineValueWorkspaceClientCapabilities)
 *
 * @since 3.17.0
 */
@Serializable
data class InlineValueWorkspaceClientCapabilities(
    /**
     * Whether the client implementation supports a refresh request sent from
     * the server to the client.
     *
     * Note that this event is global and will force the client to refresh all
     * inline values currently shown. It should be used with absolute care and
     * is useful for situations where a server, for example, detects a project
     * wide change that requires such a calculation.
     */
    var refreshSupport: Boolean? = null
)
