package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Static registration options to be returned in the initialize request.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#staticRegistrationOptions)
 */
@Serializable
sealed interface StaticRegistrationOptions {
    /**
     * The id used to register the request. The id can be used to deregister
     * the request again.
     *
     * @see Registration.id
     */
    val id: String?
}
