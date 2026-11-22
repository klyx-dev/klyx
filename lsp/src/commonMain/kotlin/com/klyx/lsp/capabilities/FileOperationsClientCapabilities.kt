package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * The client has support for file requests/notifications.
 *
 * @since 3.16.0
 */
@Serializable
data class FileOperationsClientCapabilities(
    /**
     * Whether the client supports dynamic registration for file
     * requests/notifications.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * The client has support for sending didCreateFiles notifications.
     */
    var didCreate: Boolean? = null,

    /**
     * The client has support for sending willCreateFiles requests.
     */
    var willCreate: Boolean? = null,

    /**
     * The client has support for sending didRenameFiles notifications.
     */
    var didRename: Boolean? = null,

    /**
     * The client has support for sending willRenameFiles requests.
     */
    var willRename: Boolean? = null,

    /**
     * The client has support for sending didDeleteFiles notifications.
     */
    var didDelete: Boolean? = null,

    /**
     * The client has support for sending willDeleteFiles requests.
     */
    var willDelete: Boolean? = null,
) : DynamicRegistrationCapabilities
