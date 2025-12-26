package com.klyx.lsp.server.internal

import com.klyx.lsp.CancelParams
import com.klyx.lsp.ErrorCodes
import com.klyx.lsp.IntRequestId
import com.klyx.lsp.Message
import com.klyx.lsp.NotificationMessage
import com.klyx.lsp.RequestId
import com.klyx.lsp.RequestMessage
import com.klyx.lsp.ResponseError
import com.klyx.lsp.ResponseMessage
import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.wrap
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.LSPArray
import com.klyx.lsp.types.LSPObject
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.asLeft
import com.klyx.lsp.types.asRight
import com.klyx.lsp.types.leftOr
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.concurrent.Volatile
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.typeOf

internal const val CONTENT_LEN_HEADER = "Content-Length: "
private const val CANCEL_METHOD = "$/cancelRequest"

internal class JsonRpcConnection(
    input: RawSource, // Read from client
    output: RawSink, // Write to client
    val client: LanguageClient,
    val json: Json,
    val scope: CoroutineScope
) {
    private val reader = JsonRpcReader(input.buffered(), json)
    private val writer = JsonRpcWriter(output.buffered(), json)

    private val nextId = atomic(0)
    private val pendingRequests = mutableMapOf<RequestId, CompletableDeferred<ResponseMessage>>().protect()
    private val incomingJobs = mutableMapOf<RequestId, Job>().protect()
    private val notificationHandlers = mutableMapOf<String, NotificationHandler>()
    private val requestHandlers = mutableMapOf<String, RequestHandler>()

    @Volatile
    private var started = false

    @Volatile
    private var running = false

    init {
        registerServerHandlers()
    }

    fun start() {
        check(!started) { "Connection already started" }
        started = true
        running = true

        scope.launch {
            try {
                while (isActive && running) {
                    try {
                        val message = reader.readMessage() ?: break
                        handleMessage(message)
                    } catch (_: CancellationException) {
                        break
                    } catch (err: Throwable) {
                        println("Error reading message: ${err.message}")
                        err.printStackTrace()
                    }
                }
            } finally {
                cleanup()
            }
        }
    }

    suspend fun stop() {
        running = false
        cleanup()
    }

    private suspend fun cleanup() {
        val pending = pendingRequests.withLock {
            it.values.toList().also { _ -> it.clear() }
        }
        pending.forEach {
            it.completeExceptionally(JsonRpcException("Connection closed"))
        }

        val jobs = incomingJobs.withLock {
            it.values.toList().also { _ -> it.clear() }
        }
        jobs.forEach { it.cancel("Connection closed") }
    }

    suspend fun sendRequest(method: String, params: LSPAny? = null): ResponseMessage {
        check(started) { "Connection not started" }

        val id = IntRequestId(nextId.getAndIncrement())
        val deferred = CompletableDeferred<ResponseMessage>()
        pendingRequests.withLock { it[id] = deferred }

        try {
            val requestParams = params?.let {
                when (params) {
                    is JsonArray -> params.asLeft()
                    is JsonObject -> params.asRight()
                    is JsonNull -> null
                    else -> throw IllegalArgumentException("Invalid params type: ${params::class.simpleName}")
                }
            }
            val request = RequestMessage(id, method, requestParams)
            println("sendRequest: $request")
            writer.writeMessage(request)

            return withTimeout(LSP_REQUEST_TIMEOUT) {
                deferred.await()
            }
        } catch (e: TimeoutCancellationException) {
            pendingRequests.withLock { it.remove(id) }
            cancelOutgoingRequest(id)
            throw JsonRpcException("Request timed out: $method", e)
        } catch (e: CancellationException) {
            pendingRequests.withLock { it.remove(id) }
            cancelOutgoingRequest(id)
            throw e
        } catch (e: Throwable) {
            pendingRequests.withLock { it.remove(id) }
            throw e
        }
    }

    suspend inline fun <reified T, reified P> sendRequest(
        method: String,
        params: P?
    ): T {
        val encodedParams = params?.let {
            json.encodeToJsonElement(it)
        } ?: JsonNull
        val response = sendRequest(method, encodedParams)

        val expectedType = typeOf<T>()
        val isMarkedNullable = expectedType.isMarkedNullable

        if (!isMarkedNullable && response.result == null) {
            throw JsonRpcException(
                buildString {
                    appendLine("LSP request returned null result for non-nullable type.")
                    appendLine("Method: $method")
                    appendLine("Expected type: $expectedType")
                    appendLine("Params: $encodedParams")
                    appendLine("Raw response: $response")
                    if (response.error != null) {
                        appendLine(
                            "Server error: [${response.error!!.code}] ${response.error!!.message}"
                        )
                    }
                }
            )
        }

        return try {
            json.decodeFromJsonElement(response.result ?: JsonNull)
        } catch (e: Throwable) {
            throw JsonRpcException(
                buildString {
                    appendLine("Failed to decode LSP response.")
                    appendLine("Method: $method")
                    appendLine("Expected type: $expectedType")
                    appendLine("Params: $encodedParams")
                    appendLine("Raw result: ${response.result}")
                    appendLine("Error: $e")
                },
                e
            )
        }
    }

    suspend fun sendNotification(method: String, params: LSPAny? = null) {
        check(started) { "Connection not started" }
        val notification = NotificationMessage(method, params)
        println("sendNotification: $notification")
        writer.writeMessage(notification)
    }

    fun handleServerRequest(method: String, handler: RequestHandler) {
        //check(!started) { "Handlers must be registered before start()" }
        requestHandlers[method] = handler
    }

    fun handleServerNotification(method: String, handler: NotificationHandler) {
        //check(!started) { "Handlers must be registered before start()" }
        notificationHandlers[method] = handler
    }

    private suspend fun trackIncomingRequest(id: RequestId, job: Job) {
        incomingJobs.withLock { it[id] = job }
    }

    private suspend fun cancelIncomingRequest(id: RequestId) {
        incomingJobs.withLock { it.remove(id)?.cancel("Cancelled by client (id=$id)") }
    }

    private suspend fun cancelOutgoingRequest(id: RequestId) {
        writer.writeMessage(
            NotificationMessage(
                method = CANCEL_METHOD,
                params = json.encodeToJsonElement(CancelParams(id))
            )
        )
    }

    private fun registerServerHandlers() {
        registerServerNotificationHandlers()
        registerServerRequestHandlers()
    }

    private fun registerServerNotificationHandlers() {
        handleServerNotification(CANCEL_METHOD) { _, notification ->
            val params = json.decodeFromJsonElement<CancelParams>(notification.params!!)
            cancelIncomingRequest(params.id)
        }
        client.registerServerNotificationHandlers()
    }

    private fun registerServerRequestHandlers() {
        client.registerServerRequestHandlers()
    }

    private suspend fun handleMessage(message: Message) {
        when (message) {
            is NotificationMessage -> handleNotification(message)
            is RequestMessage -> handleRequest(message)
            is ResponseMessage -> handleResponse(message)
        }
    }

    private suspend fun handleRequest(request: RequestMessage) {
        val job = scope.launch {
            try {
                val handler = requestHandlers[request.method]
                if (handler == null) {
                    sendResponseError(
                        id = request.id,
                        code = ErrorCodes.MethodNotFound,
                        message = "Method not found: ${request.method}"
                    )
                    return@launch
                }

                val response = handler(request)
                if (isActive) {
                    writer.writeMessage(response)
                }
            } catch (_: CancellationException) {
                // Request was cancelled, don't send response
            } catch (err: Throwable) {
                if (isActive) {
                    sendResponseError(
                        id = request.id,
                        code = ErrorCodes.InternalError,
                        message = "Internal error: ${err.message}"
                    )
                }
            } finally {
                incomingJobs.withLock { it.remove(request.id) }
            }
        }

        trackIncomingRequest(request.id, job)
    }

    private suspend fun handleResponse(response: ResponseMessage) {
        println("handleResponse: $response")
        val id = response.id ?: return
        val deferred = pendingRequests.withLock { it.remove(id) } ?: return

        if (response.error != null) {
            deferred.completeExceptionally(response.error!!.wrap())
        } else {
            deferred.complete(response)
        }
    }

    private suspend fun handleNotification(notification: NotificationMessage) {
        try {
            val handler = notificationHandlers[notification.method]
            if (handler != null) {
                handler(null, notification)
            }
        } catch (err: Throwable) {
            //println("Error handling notification ${notification.method}: ${err.message}")
            //err.printStackTrace()
            throw err as? JsonRpcException ?: JsonRpcException("Error handling notification", err)
        }
    }

    private suspend fun sendResponseError(
        id: RequestId,
        code: Int,
        message: String,
        data: LSPAny? = null
    ) {
        val error = ResponseError(code, message, data)
        val response = ResponseMessage(id, null, error)
        writer.writeMessage(response)
    }
}

context(connection: JsonRpcConnection)
private fun LanguageClient.registerServerNotificationHandlers() {
    fun handleNotification(method: String, handler: suspend (NotificationMessage) -> Unit) {
        connection.handleServerNotification(method) { _, notification ->
            handler(notification)
            println("handleNotification: $notification")
        }
    }

    with(connection) {
        handleNotification("$/progress") { notification ->
            notification.params?.let { notifyProgress(json.decodeFromJsonElement(it)) }
        }

        handleNotification("$/logTrace") {
            logTrace(json.decodeFromJsonElement(it.params!!))
        }
        handleNotification("window/showMessage") {
            showMessage(json.decodeFromJsonElement(it.params!!))
        }
        handleNotification("telemetry/event") {
            telemetryEvent(json.decodeFromJsonElement(it.params!!))
        }
        handleNotification("textDocument/publishDiagnostics") {
            publishDiagnostics(json.decodeFromJsonElement(it.params!!))
        }
    }
}

context(connection: JsonRpcConnection)
private fun LanguageClient.registerServerRequestHandlers() {
    fun handleRequest(method: String, handler: suspend (RequestId, JsonElement?) -> ResponseMessage) {
        connection.handleServerRequest(method) {
            val params = it.params
            handler(it.id, params?.jsonElement())
        }
    }

    with(connection) {
        handleRequest("client/registerCapability") { id, params ->
            handleVoidRequest(id, params, ::registerCapability)
        }

        handleRequest("client/unregisterCapability") { id, params ->
            handleVoidRequest(id, params, ::unregisterCapability)
        }

        handleRequest("workspace/codeLens/refresh") { id, _ ->
            handleVoidNoParamsRequest(id, ::refreshCodeLenses)
        }
        handleRequest("workspace/foldingRange/refresh") { id, _ ->
            handleVoidNoParamsRequest(id, ::refreshFoldingRanges)
        }
        handleRequest("workspace/semanticTokens/refresh") { id, _ ->
            handleVoidNoParamsRequest(id, ::refreshSemanticTokens)
        }
        handleRequest("workspace/inlayHint/refresh") { id, _ ->
            handleVoidNoParamsRequest(id, ::refreshInlayHints)
        }
        handleRequest("workspace/inlineValue/refresh") { id, _ ->
            handleVoidNoParamsRequest(id, ::refreshInlineValues)
        }
        handleRequest("workspace/diagnostic/refresh") { id, _ ->
            handleVoidNoParamsRequest(id, ::refreshDiagnostics)
        }
        handleRequest("workspace/textDocumentContent/refresh") { id, params ->
            handleVoidRequest(id, params, ::refreshTextDocumentContent)
        }
        handleRequest("workspace/configuration") { id, params ->
            try {
                val params = params ?: return@handleRequest invalidParams(id, params)
                val result = configuration(json.decodeFromJsonElement(params))
                createResponse(id, json.encodeToJsonElement(result))
            } catch (e: Throwable) {
                errorResponse(
                    id,
                    ErrorCodes.InternalError,
                    "Internal error: $e"
                )
            }
        }
        handleRequest("workspace/workspaceFolders") { id, _ ->
            val result = try {
                workspaceFolders()
            } catch (e: Throwable) {
                return@handleRequest errorResponse(
                    id,
                    ErrorCodes.InternalError,
                    "Internal error: $e"
                )
            }
            createResponse(id, json.encodeToJsonElement(result))
        }
        handleRequest("workspace/applyEdit") { id, params ->
            val params = params ?: return@handleRequest invalidParams(id, params)
            val result = try {
                applyEdit(json.decodeFromJsonElement(params))
            } catch (e: Throwable) {
                return@handleRequest errorResponse(
                    id,
                    ErrorCodes.InternalError,
                    "Internal error: $e"
                )
            }
            createResponse(id, json.encodeToJsonElement(result))
        }
        handleRequest("window/showMessageRequest") { id, params ->
            val params = params ?: return@handleRequest invalidParams(id, params)
            val result = try {
                showMessageRequest(json.decodeFromJsonElement(params))
            } catch (e: Throwable) {
                return@handleRequest errorResponse(
                    id,
                    ErrorCodes.InternalError,
                    "Internal error: $e"
                )
            }
            createResponse(id, json.encodeToJsonElement(result))
        }
        handleRequest("window/showDocument") { id, params ->
            val params = params ?: return@handleRequest invalidParams(id, params)
            val result = try {
                showDocument(json.decodeFromJsonElement(params))
            } catch (e: Throwable) {
                return@handleRequest errorResponse(
                    id,
                    ErrorCodes.InternalError,
                    "Internal error: $e"
                )
            }
            createResponse(id, json.encodeToJsonElement(result))
        }
        handleRequest("window/workDoneProgress/create") { id, params ->
            handleVoidRequest(id, params, ::createProgress)
        }
    }
}

private suspend inline fun handleVoidNoParamsRequest(
    id: RequestId,
    action: suspend () -> Unit
): ResponseMessage {
    return try {
        action()
        createResponse(id, JsonNull)
    } catch (e: Throwable) {
        errorResponse(
            id,
            ErrorCodes.InternalError,
            "Internal error: ${e.message}"
        )
    }
}

@OptIn(ExperimentalTypeInference::class)
context(connection: JsonRpcConnection)
private suspend inline fun <reified P> handleVoidRequest(
    id: RequestId,
    params: JsonElement?,
    @BuilderInference
    action: suspend (P) -> Unit
): ResponseMessage {
    return try {
        val decoded: P = connection.json.decodeFromJsonElement(params ?: JsonNull)
        action(decoded)
        createResponse(id, JsonNull)
    } catch (e: Throwable) {
        errorResponse(
            id,
            ErrorCodes.InternalError,
            "Internal error: $e"
        )
    }
}

private fun invalidParams(id: RequestId, params: LSPAny? = null): ResponseMessage {
    return errorResponse(id, ErrorCodes.InvalidParams, "Invalid params", params)
}

private fun errorResponse(id: RequestId, code: Int, message: String, data: LSPAny? = null): ResponseMessage {
    val error = ResponseError(code, message, data)
    return ResponseMessage(id, null, error)
}

private fun createResponse(id: RequestId?, result: LSPAny? = null, error: ResponseError? = null): ResponseMessage {
    val response = if (error != null) {
        ResponseMessage(id, null, error)
    } else {
        ResponseMessage(id, result, null)
    }
    return response
}

private fun OneOf<LSPArray, LSPObject>.jsonElement() = leftOr { right }
