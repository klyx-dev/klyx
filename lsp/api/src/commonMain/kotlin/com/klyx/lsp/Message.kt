package com.klyx.lsp

import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.LSPArray
import com.klyx.lsp.types.LSPObject
import com.klyx.lsp.types.NumberOrString
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.asLeft
import com.klyx.lsp.types.asRight
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal const val JSON_RPC_VERSION = "2.0"

/**
 * A general message as defined by JSON-RPC. The language server
 * protocol always uses “2.0” as the jsonrpc version.
 */
@Serializable(MessageSerializer::class)
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

@Suppress("FunctionName")
fun IntRequestId(id: Int): RequestId = id.asLeft()

@Suppress("FunctionName")
fun StringRequestId(id: String): RequestId = id.asRight()

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
    /**
     * The method to be invoked.
     */
    val method: String,
    /**
     * The notification's params.
     */
    val params: LSPAny? = null,

    override val jsonrpc: String = JSON_RPC_VERSION,
) : Message

@Serializable
data class CancelParams(
    /**
     * The request id to cancel.
     */
    val id: RequestId
)

internal object MessageSerializer : JsonContentPolymorphicSerializer<Message>(Message::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Message> {
        val obj = element.jsonObject

        return when {
            "result" in obj || "error" in obj -> ResponseMessage.serializer()
            "id" in obj && "method" in obj -> RequestMessage.serializer()
            "method" in obj -> NotificationMessage.serializer()
            else -> error("Invalid JSON-RPC message: $obj")
        }
    }
}
