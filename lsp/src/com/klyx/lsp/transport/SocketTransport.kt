package com.klyx.lsp.transport

import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.LanguageServer
import com.klyx.lsp.server.createLanguageServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.io.RawSink
import kotlinx.io.RawSource

data class SocketStreams(
    val source: RawSource,
    val sink: RawSink,
    val closeable: AutoCloseable,
)

/**
 * Connect to a TCP socket as a client with retry support.
 */
expect suspend fun connectSocketStreams(
    host: String,
    port: Int,
    timeoutMs: Long = 10_000,
    retryIntervalMs: Long = 500,
): SocketStreams

/**
 * Listen on a local TCP port and accept the first incoming connection.
 * If [port] is 0, an ephemeral available port is chosen by the OS.
 */
expect suspend fun listenSocketStreams(
    host: String = "127.0.0.1",
    port: Int = 0,
    timeoutMs: Long = 15_000,
    onListening: ((assignedPort: Int) -> Unit)? = null,
): SocketStreams

class LspSocketServer(
    val host: String,
    val port: Int,
    private val closeable: AutoCloseable,
    val server: LanguageServer,
) : LspServerConnection, LanguageServer by server {
    override fun close() {
        try {
            closeable.close()
        } catch (_: Throwable) {}
    }
}

/**
 * Connects to an LSP server running over a TCP socket (as client).
 */
suspend fun connectLspSocket(
    host: String = "127.0.0.1",
    port: Int,
    client: LanguageClient,
    timeoutMs: Long = 10_000,
    retryIntervalMs: Long = 500,
): LspSocketServer {
    (val source, val sink, val closeable) = connectSocketStreams(host, port, timeoutMs, retryIntervalMs)
    val server = createLanguageServer(client, source, sink)
    return LspSocketServer(host, port, closeable, server)
}

/**
 * Listens on a local port and connects to the first client that connects (e.g. from Termux).
 */
suspend fun listenLspSocket(
    host: String = "127.0.0.1",
    port: Int = 0,
    client: LanguageClient,
    timeoutMs: Long = 15_000,
    onListening: ((assignedPort: Int) -> Unit)? = null,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
): LspSocketServer {
    var actualPort = port
    (val source, val sink, val closeable) = listenSocketStreams(host, port, timeoutMs) { assignedPort ->
        actualPort = assignedPort
        onListening?.invoke(assignedPort)
    }
    val server = createLanguageServer(client, source, sink)
    return LspSocketServer(host, actualPort, closeable, server)
}
