package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceSymbolOptions)
 */
@Serializable
open class WorkspaceSymbolOptions(
    /**
     * The server provides support to resolve additional
     * information for a workspace symbol.
     *
     * @since 3.17.0
     */
    var resolveProvider: Boolean? = null
) : WorkDoneProgressOptions()

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceSymbolRegistrationOptions)
 */
@Serializable
class WorkspaceSymbolRegistrationOptions : WorkspaceSymbolOptions()
