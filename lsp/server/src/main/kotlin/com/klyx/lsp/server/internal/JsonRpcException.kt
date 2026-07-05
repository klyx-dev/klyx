package com.klyx.lsp.server.internal

/**
 * Exception thrown for JSON-RPC errors.
 */
internal class JsonRpcException(
    override val message: String?,
    override val cause: Throwable? = null
) : Exception(message, cause)
