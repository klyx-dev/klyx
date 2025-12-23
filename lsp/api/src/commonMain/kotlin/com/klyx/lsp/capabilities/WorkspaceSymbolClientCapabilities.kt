package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspace_symbol)
 */
@Serializable
data class WorkspaceSymbolClientCapabilities(
    /**
     * Symbol request supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Specific capabilities for the `SymbolKind` in the `workspace/symbol`
     * request.
     */
    var symbolKind: SymbolKindClientCapabilities? = null,

    /**
     * The client supports tags on `SymbolInformation` and `WorkspaceSymbol`.
     * Clients supporting tags have to handle unknown tags gracefully.
     *
     * @since 3.16.0
     */
    var tagSupport: SymbolTagSupportClientCapabilities? = null,

    /**
     * The client supports partial workspace symbols. The client will send the
     * request `workspaceSymbol/resolve` to the server to resolve additional
     * properties.
     *
     * @since 3.17.0 - proposedState
     */
    var resolveSupport: ResolveSupportClientCapabilities? = null
) : DynamicRegistrationCapabilities
