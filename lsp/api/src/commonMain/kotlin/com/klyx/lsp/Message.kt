package com.klyx.lsp

import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.LSPArray
import com.klyx.lsp.types.LSPObject
import com.klyx.lsp.types.NumberOrString
import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable

const val JSON_RPC_VERSION = "2.0"
const val CONTENT_LEN_HEADER = "Content-Length: "

/**
 * A general message as defined by JSON-RPC. The language server
 * protocol always uses “2.0” as the jsonrpc version.
 */
@Serializable
sealed interface Message {
    val jsonrpc: String
}

@Serializable
data class RequestMessage(
    /**
     * The request id.
     */
    val id: RequestId,

    /**
     * The method to be invoked.
     */
    val method: String,

    /**
     * The method's params.
     */
    val params: OneOf<LSPArray, LSPObject>? = null,
    override val jsonrpc: String = JSON_RPC_VERSION
) : Message

/**
 * The request id.
 */
typealias RequestId = NumberOrString

@Serializable
data class ResponseMessage(
    /**
     * The request id.
     */
    val id: RequestId?,

    /**
     * The result of a request. This member is REQUIRED on success.
     * This member MUST NOT exist if there was an error invoking the method.
     */
    val result: LSPAny? = null,

    /**
     * The error object in case a request fails.
     */
    val error: ResponseError? = null,
    override val jsonrpc: String = JSON_RPC_VERSION
) : Message

@Serializable
data class ResponseError(
    /**
     * A number indicating the error type that occurred.
     */
    val code: Int,

    /**
     * A string providing a short description of the error.
     */
    val message: String,

    /**
     * A primitive or structured value that contains additional
     * information about the error. Can be omitted.
     */
    var data: LSPAny? = null
)

@Serializable
data class NotificationMessage(
    override val jsonrpc: String,
    /**
     * The method to be invoked.
     */
    val method: String,

    /**
     * The notification's params.
     */
    val params: LSPAny? = null,
) : Message

@Serializable
data class CancelParams(
    /**
     * The request id to cancel.
     */
    val id: RequestId
)

