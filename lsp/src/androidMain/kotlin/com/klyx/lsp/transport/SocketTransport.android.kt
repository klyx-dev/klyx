package com.klyx.lsp.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.time.Duration.Companion.milliseconds

actual suspend fun connectSocketStreams(
    host: String,
    port: Int,
    timeoutMs: Long,
    retryIntervalMs: Long,
): SocketStreams = withContext(Dispatchers.IO) {
    val deadline = System.currentTimeMillis() + timeoutMs
    var lastException: Throwable? = null

    while (System.currentTimeMillis() < deadline) {
        try {
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(host, port), 2000)
            return@withContext SocketStreams(
                source = socket.getInputStream().asSource(),
                sink = socket.getOutputStream().asSink(),
                closeable = socket,
            )
        } catch (e: IOException) {
            lastException = e
            delay(retryIntervalMs.milliseconds)
        }
    }

    throw IOException(
        "Failed to connect to LSP socket at $host:$port after ${timeoutMs}ms",
        lastException,
    )
}

actual suspend fun listenSocketStreams(
    host: String,
    port: Int,
    timeoutMs: Long,
    onListening: ((assignedPort: Int) -> Unit)?,
): SocketStreams = withContext(Dispatchers.IO) {
    val serverSocket = ServerSocket(port, 1, InetAddress.getByName(host))
    onListening?.invoke(serverSocket.localPort)
    try {
        val boundedTimeout = timeoutMs.coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
        serverSocket.soTimeout = boundedTimeout
        val socket = serverSocket.accept()
        socket.tcpNoDelay = true
        SocketStreams(
            source = socket.getInputStream().asSource(),
            sink = socket.getOutputStream().asSink(),
            closeable = {
                try { socket.close() } catch (_: Throwable) {}
                try { serverSocket.close() } catch (_: Throwable) {}
            },
        )
    } catch (e: Throwable) {
        try { serverSocket.close() } catch (_: Throwable) {}
        throw e
    }
}
