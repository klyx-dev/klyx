package com.klyx.lsp.transport

actual suspend fun connectSocketStreams(
    host: String,
    port: Int,
    timeoutMs: Long,
    retryIntervalMs: Long,
): SocketStreams {
    error("Socket transport is not supported on iOS yet")
}

actual suspend fun listenSocketStreams(
    host: String,
    port: Int,
    timeoutMs: Long,
    onListening: ((assignedPort: Int) -> Unit)?,
): SocketStreams {
    error("Socket transport is not supported on iOS yet")
}
