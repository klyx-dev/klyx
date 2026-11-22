package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * General parameters to register for a capability.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#registration)
 */
@Serializable
data class Registration(
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again.
     */
    val id: String,

    /**
     * The method / capability to register for.
     */
    val method: String,

    /**
     * Options necessary for the registration.
     */
    var registerOptions: JsonElement? = null
)

/**
 * General parameters to unregister a capability.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#unregistration)
 */
@Serializable
data class Unregistration(
    /**
     * The id used to unregister the request or notification. Usually an id
     * provided during the register request.
     */
    val id: String,

    /**
     * The method / capability to unregister for.
     */
    val method: String
)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#registrationParams)
 */
@Serializable
data class RegistrationParams(val registrations: List<Registration>)

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#unregistrationParams)
 */
@Serializable
data class UnregistrationParams(
    // This should correctly be named `unregistrations`. However, changing this
    // is a breaking change and needs to wait until we deliver a 4.x version
    // of the specification.
    val unregisterations: List<Unregistration>
)
