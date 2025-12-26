package com.klyx.lsp.capabilities

import com.klyx.lsp.PrepareSupportDefaultBehavior
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#renameClientCapabilities)
 */
@Serializable
data class RenameCapabilities(
    /**
     * Whether rename supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Client supports testing for validity of rename operations
     * before execution.
     *
     * @since version 3.12.0
     */
    var prepareSupport: Boolean? = null,


    /**
     * Client supports the default behavior result
     * (`{ defaultBehavior: boolean }`).
     *
     * The value indicates the default behavior used by the
     * client.
     *
     * @since version 3.16.0
     */
    var prepareSupportDefaultBehavior: PrepareSupportDefaultBehavior? = null,

    /**
     * Whether the client honors the change annotations in
     * text edits and resource operations returned via the
     * rename request's workspace edit by, for example, presenting
     * the workspace edit in the user interface and asking
     * for confirmation.
     *
     * @since 3.16.0
     */
    var honorsChangeAnnotations: Boolean? = null
) : DynamicRegistrationCapabilities
