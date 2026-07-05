package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Workspace specific server capabilities
 */
@Serializable
data class WorkspaceServerCapabilities(
    /**
     * The server supports workspace folder.
     *
     * @since 3.6.0
     */
    val workspaceFolders: WorkspaceFoldersServerCapabilities?,

    /**
     * The server is interested in file notifications/requests.
     *
     * @since 3.16.0
     */
    val fileOperations: FileOperationsServerCapabilities?
)
