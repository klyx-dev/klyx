package com.klyx.lsp.capabilities

import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceFoldersServerCapabilities)
 */
@Serializable
data class WorkspaceFoldersServerCapabilities(
    /**
     * The server has support for workspace folders.
     */
    var supported: Boolean? = null,

    /**
     * Whether the server wants to receive workspace folder
     * change notifications.
     *
     * If a string is provided, the string is treated as an ID
     * under which the notification is registered on the client
     * side. The ID can be used to unregister for these events
     * using the `client/unregisterCapability` request.
     */
    var changeNotifications: OneOf<String, Boolean>? = null
)
