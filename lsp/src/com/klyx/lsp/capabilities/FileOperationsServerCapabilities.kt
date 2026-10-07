package com.klyx.lsp.capabilities

import com.klyx.lsp.FileOperationRegistrationOptions
import kotlinx.serialization.Serializable

/**
 * The server is interested in file notifications/requests.
 *
 * @since 3.16.0
 */
@Serializable
data class FileOperationsServerCapabilities(
    /**
     * The server is interested in receiving didCreateFiles
     * notifications.
     */
    val didCreate: FileOperationRegistrationOptions?,

    /**
     * The server is interested in receiving willCreateFiles requests.
     */
    val willCreate: FileOperationRegistrationOptions?,

    /**
     * The server is interested in receiving didRenameFiles
     * notifications.
     */
    val didRename: FileOperationRegistrationOptions?,

    /**
     * The server is interested in receiving willRenameFiles requests.
     */
    val willRename: FileOperationRegistrationOptions?,

    /**
     * The server is interested in receiving didDeleteFiles file
     * notifications.
     */
    val didDelete: FileOperationRegistrationOptions?,

    /**
     * The server is interested in receiving willDeleteFiles file
     * requests.
     */
    val willDelete: FileOperationRegistrationOptions?,
)
