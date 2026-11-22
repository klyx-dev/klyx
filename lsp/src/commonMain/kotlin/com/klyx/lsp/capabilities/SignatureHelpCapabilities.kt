package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#signatureHelpClientCapabilities)
 */
@Serializable
data class SignatureHelpCapabilities(
    /**
     * Whether signature help supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * The client supports the following `SignatureInformation`
     * specific properties.
     */
    var signatureInformation: SignatureInformationCapabilities? = null,

    /**
     * The client supports sending additional context information for a
     * `textDocument/signatureHelp` request. A client that opts into
     * contextSupport will also support the `retriggerCharacters` on
     * `SignatureHelpOptions`.
     *
     * @since 3.15.0
     */
    var contextSupport: Boolean? = null
) : DynamicRegistrationCapabilities
