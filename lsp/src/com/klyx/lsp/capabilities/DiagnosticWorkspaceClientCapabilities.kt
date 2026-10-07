package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Workspace client capabilities specific to diagnostic pull requests.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#diagnosticWorkspaceClientCapabilities)
 *
 * @since 3.17.0
 */
@Serializable
data class DiagnosticWorkspaceClientCapabilities(
    /**
     * Whether the client implementation supports a refresh request sent from
     * the server to the client.
     *
     * Note that this event is global and will force the client to refresh all
     * pulled diagnostics currently shown. It should be used with absolute care
     * and is useful for situation where a server, for example, detects a project
     * wide change that requires such a calculation.
     */
    var refreshSupport: Boolean? = null
)
