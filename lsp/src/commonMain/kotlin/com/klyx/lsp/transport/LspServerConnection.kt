package com.klyx.lsp.transport

import com.klyx.lsp.server.LanguageServer

/**
 * Common abstraction representing an active connection to an LSP server,
 * regardless of underlying transport (process stdio, TCP socket, Termux, etc.).
 */
interface LspServerConnection : LanguageServer, AutoCloseable
