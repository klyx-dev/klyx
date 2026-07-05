package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokensWorkspaceClientCapabilities)
 */
@Serializable
data class SemanticTokensWorkspaceClientCapabilities(
    /**
     * Whether the client implementation supports a refresh request sent from
     * the server to the client.
     *
     * Note that this event is global and will force the client to refresh all
     * semantic tokens currently shown. It should be used with absolute care
     * and is useful for situation where a server, for example, detects a project
     * wide change that requires such a calculation.
     */
    var refreshSupport: Boolean? = null
)
