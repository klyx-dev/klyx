package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Window specific client capabilities.
 */
@Serializable
data class WindowClientCapabilities(
    /**
     * It indicates whether the client supports server initiated
     * progress using the `window/workDoneProgress/create` request.
     *
     * The capability also controls Whether client supports handling
     * of progress notifications. If set servers are allowed to report a
     * `workDoneProgress` property in the request specific server
     * capabilities.
     *
     * @since 3.15.0
     */
    var workDoneProgress: Boolean? = null,

    /**
     * Capabilities specific to the showMessage request
     *
     * @since 3.16.0
     */
    var showMessage: ShowMessageRequestClientCapabilities? = null,

    /**
     * Client capabilities for the show document request.
     *
     * @since 3.16.0
     */
    var showDocument: ShowDocumentClientCapabilities? = null
)
