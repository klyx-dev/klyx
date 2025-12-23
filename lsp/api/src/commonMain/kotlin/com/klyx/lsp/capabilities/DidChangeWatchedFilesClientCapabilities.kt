package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didChangeWatchedFilesClientCapabilities)
 */
@Serializable
data class DidChangeWatchedFilesClientCapabilities(
    /**
     * Did change watched files notification supports dynamic registration.
     * Please note that the current protocol doesn't support static
     * configuration for file changes from the server side.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Whether the client has support for relative patterns
     * or not.
     *
     * @since 3.17.0
     */
    var relativePatternSupport: Boolean? = null
) : DynamicRegistrationCapabilities

