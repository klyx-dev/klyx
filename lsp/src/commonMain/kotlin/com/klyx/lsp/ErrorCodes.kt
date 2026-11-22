package com.klyx.lsp

@Suppress("ConstPropertyName")
object ErrorCodes {
    // Defined by JSON-RPC
    const val ParseError = -32700
    const val InvalidRequest = -32600
    const val MethodNotFound = -32601
    const val InvalidParams = -32602
    const val InternalError = -32603

    /**
     * This is the start range of JSON-RPC reserved error codes.
     * It doesn't denote a real error code. No LSP error codes should
     * be defined between the start and end range. For backwards
     * compatibility the `ServerNotInitialized` and the `UnknownErrorCode`
     * are left in the range.
     *
     * @since 3.16.0
     */
    const val jsonrpcReservedErrorRangeStart = -32099

    @Deprecated("use jsonrpcReservedErrorRangeStart", ReplaceWith("jsonrpcReservedErrorRangeStart"))
    const val serverErrorStart = jsonrpcReservedErrorRangeStart

    /**
     * Error code indicating that a server received a notification or
     * request before the server received the `initialize` request.
     */
    const val ServerNotInitialized = -32002
    const val UnknownErrorCode = -32001

    /**
     * This is the end range of JSON-RPC reserved error codes.
     * It doesn't denote a real error code.
     *
     * @since 3.16.0
     */
    const val jsonrpcReservedErrorRangeEnd = -32000

    @Deprecated("use jsonrpcReservedErrorRangeEnd", ReplaceWith("jsonrpcReservedErrorRangeEnd"))
    const val serverErrorEnd = jsonrpcReservedErrorRangeEnd

    /**
     * This is the start range of LSP reserved error codes.
     * It doesn't denote a real error code.
     *
     * @since 3.16.0
     */
    const val lspReservedErrorRangeStart = -32899

    /**
     * A request failed but it was syntactically correct, e.g the
     * method name was known and the parameters were valid. The error
     * message should contain human readable information about why
     * the request failed.
     *
     * @since 3.17.0
     */
    const val RequestFailed = -32803

    /**
     * The server cancelled the request. This error code should
     * only be used for requests that explicitly support being
     * server cancellable.
     *
     * @since 3.17.0
     */
    const val ServerCancelled = -32802

    /**
     * The server detected that the content of a document got
     * modified outside normal conditions. A server should
     * NOT send this error code if it detects a content change
     * in its unprocessed messages. The result even computed
     * on an older state might still be useful for the client.
     *
     * If a client decides that a result is not of any use anymore
     * the client should cancel the request.
     */
    const val ContentModified = -32801

    /**
     * The client has canceled a request and a server has detected
     * the cancel.
     */
    const val RequestCancelled = -32800

    /**
     * This is the end range of LSP reserved error codes.
     * It doesn't denote a real error code.
     *
     * @since 3.16.0
     */
    const val lspReservedErrorRangeEnd = -32800
}
