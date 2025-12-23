package com.klyx.lsp

import com.klyx.lsp.types.URI
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceFolder)
 */
@Serializable
data class WorkspaceFolder(
    /**
     * The associated URI for this workspace folder.
     */
    val uri: URI,

    /**
     * The name of the workspace folder. Used to refer to this
     * workspace folder in the user interface.
     */
    val name: String
)
