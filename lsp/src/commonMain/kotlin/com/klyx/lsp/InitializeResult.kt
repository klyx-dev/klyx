package com.klyx.lsp

import com.klyx.lsp.capabilities.ServerCapabilities
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#initializeResult)
 *
 * @property capabilities The capabilities the language server provides.
 * @property serverInfo Information about the server.
 */
@Serializable
data class InitializeResult(
    /**
     * The capabilities the language server provides.
     */
    val capabilities: ServerCapabilities,

    /**
     * Information about the server.
     *
     * @since 3.15.0
     */
    val serverInfo: ServerInfo? = null
)

/**
 * Information about the server.
 *
 * @since 3.15.0
 */
@Serializable
data class ServerInfo(
    /**
     * The name of the server as defined by the server.
     */
    val name: String,

    /**
     * The server's version as defined by the server.
     */
    val version: String? = null
)
