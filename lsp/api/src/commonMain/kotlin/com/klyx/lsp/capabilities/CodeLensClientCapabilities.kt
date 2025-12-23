package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#codeLensClientCapabilities)
 */
@Serializable
data class CodeLensClientCapabilities(
    /**
     * Whether code lens supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Whether the client supports resolving additional code lens
     * properties via a separate `codeLens/resolve` request.
     *
     * @since 3.18.0
     */
    var resolveSupport: ClientCodeLensResolveOptions? = null
) : DynamicRegistrationCapabilities

/**
 * @since 3.18.0
 */
@Serializable
data class ClientCodeLensResolveOptions(
    /**
     * The properties that a client can resolve lazily.
     */
    val properties: List<String>
)
