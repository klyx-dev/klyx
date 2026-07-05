package com.klyx.api.lsp

import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.LanguageServer

/**
 * Provider for a Language Server.
 *
 * Plugins implement this interface to provide LSP support for specific file types.
 */
fun interface LanguageServerProvider {

    /**
     * Starts a new [LanguageServer].
     *
     * The implementation is responsible for managing the server process or connection
     * and returning a [LanguageServer] instance that communicates with the provided [client].
     *
     * @param client The client implementation to communicate back to the editor.
     * @return A [LanguageServer] proxy.
     */
    suspend fun startServer(client: LanguageClient): LanguageServer
}

/**
 * Handle for a registered Language Server.
 */
interface LanguageServerRegistration {
    /**
     * Unregisters the Language Server.
     */
    fun unregister()
}
