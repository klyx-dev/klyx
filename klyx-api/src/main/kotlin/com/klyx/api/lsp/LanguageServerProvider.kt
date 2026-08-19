package com.klyx.api.lsp

import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.LanguageServer
import com.klyx.lsp.types.LSPAny

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

    /**
     * Optional configuration sent to the server in the `initialize` request.
     *
     * Plugins may override this to pass server-specific options
     * (e.g. `rust-analyzer` settings). Defaults to `null`.
     */
    fun initializationOptions(): LSPAny? = null
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
