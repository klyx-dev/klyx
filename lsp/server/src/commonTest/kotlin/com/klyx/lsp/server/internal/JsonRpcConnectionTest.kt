package com.klyx.lsp.server.internal

import com.klyx.lsp.CancelParams
import com.klyx.lsp.ConfigurationParams
import com.klyx.lsp.ErrorCodes
import com.klyx.lsp.IntRequestId
import com.klyx.lsp.LogMessageParams
import com.klyx.lsp.MessageActionItem
import com.klyx.lsp.NotificationMessage
import com.klyx.lsp.PublishDiagnosticsParams
import com.klyx.lsp.RequestMessage
import com.klyx.lsp.ResponseError
import com.klyx.lsp.ResponseMessage
import com.klyx.lsp.ShowMessageParams
import com.klyx.lsp.ShowMessageRequestParams
import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.ResponseErrorException
import com.klyx.lsp.types.LSPAny
import com.klyx.lsp.types.LSPArray
import com.klyx.lsp.types.LSPObject
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.asRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.readTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

class JsonRpcConnectionTest : FunSpec({

    val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    data class TestSetup(
        val connection: JsonRpcConnection,
        val clientToServer: TestStream,
        val serverToClient: TestStream,
        val scope: CoroutineScope,
        val startJob: Job
    )

    suspend fun createTestSetup(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)): TestSetup {
        val clientToServer = TestStream()
        val serverToClient = TestStream()
        val client = object : LanguageClient {
            override suspend fun publishDiagnostics(params: PublishDiagnosticsParams) {
                TODO("Not yet implemented")
            }

            override suspend fun configuration(params: ConfigurationParams): List<LSPAny> {
                TODO("Not yet implemented")
            }

            override suspend fun showMessage(params: ShowMessageParams) {
                TODO("Not yet implemented")
            }

            override suspend fun showMessageRequest(params: ShowMessageRequestParams): MessageActionItem? {
                TODO("Not yet implemented")
            }

            override suspend fun logMessage(params: LogMessageParams) {
                TODO("Not yet implemented")
            }

            override suspend fun telemetryEvent(params: OneOf<LSPObject, LSPArray>) {
                TODO("Not yet implemented")
            }
        }
        val scope = scope + SupervisorJob()

        val connection = JsonRpcConnection(
            input = clientToServer.asSource(),
            output = serverToClient.asSink(),
            client = client,
            json = json,
            scope = scope
        )

        val startJob = scope.launch {
            connection.start()
        }

        delay(100) // Let connection start

        return TestSetup(connection, clientToServer, serverToClient, scope, startJob)
    }

    suspend fun TestSetup.cleanup() {
        startJob.cancel()
        scope.cancel()
        clientToServer.close()
        serverToClient.close()
        delay(50)
    }

    test("should send notification") {
        val setup = createTestSetup()

        setup.connection.sendNotification(
            method = "test/notification",
            params = buildJsonObject { put("data", "test") }
        )

        val written = setup.serverToClient.readAll()
        written shouldContain "test/notification"
        written shouldContain "\"data\":\"test\""
        println(written)

        //setup.cleanup()
    }

    test("should handle incoming notification") {
        val setup = createTestSetup()

        var handlerCalled = false
        var receivedMethod = ""

        setup.connection.handleServerNotification("test/notification") { _, notification ->
            handlerCalled = true
            receivedMethod = notification.method
        }

        val notification = NotificationMessage(
            method = "test/notification",
            params = buildJsonObject { put("data", "test") }
        )
        val notificationJson = json.encodeToString(notification)
        setup.clientToServer.writeMessage(notificationJson)

        delay(100)
        handlerCalled shouldBe true
        receivedMethod shouldBe "test/notification"

        //setup.cleanup()
    }

    test("should handle incoming request and send response") {
        val setup = createTestSetup()

        var handlerCalled = false

        setup.connection.handleServerRequest("test/request") { request ->
            handlerCalled = true
            ResponseMessage(
                id = request.id,
                result = buildJsonObject { put("handled", true) },
                error = null
            )
        }

        val request = RequestMessage(
            id = IntRequestId(1),
            method = "test/request",
            params = buildJsonObject { put("param", "value") }.asRight()
        )
        val requestJson = json.encodeToString(request)
        setup.clientToServer.writeMessage(requestJson)

        delay(100)

        handlerCalled shouldBe true
        val written = setup.serverToClient.readAll()
        written shouldContain "\"handled\":true"

        //setup.cleanup()
    }

    test("should send request and receive response") {
        val setup = createTestSetup()

        // Simulate server responding
        val responseJob = setup.scope.launch {
            delay(150)

            val response = ResponseMessage(
                id = IntRequestId(0),
                result = buildJsonObject { put("status", "ok") },
                error = null
            )
            val responseJson = json.encodeToString(response)
            setup.clientToServer.writeMessage(responseJson)
        }

        val result = setup.connection.sendRequest(
            method = "test/method",
            params = buildJsonObject { put("key", "value") }
        )

        result.id shouldBe IntRequestId(0)
        result.result.shouldNotBeNull()
        result.error.shouldBeNull()

        responseJob.cancel()
        //setup.cleanup()
    }

    test("should return method not found for unknown request") {
        val setup = createTestSetup()

        val request = RequestMessage(
            id = IntRequestId(1),
            method = "unknown/method",
            params = null
        )
        val requestJson = json.encodeToString(request)
        setup.clientToServer.writeMessage(requestJson)

        delay(300)

        val written = setup.serverToClient.readAll()
        written shouldContain "Method not found"
        written shouldContain "unknown/method"
        println(written)

        //setup.cleanup()
    }

    test("should handle request cancellation") {
        val setup = createTestSetup()

        var jobCancelled = false

        setup.connection.handleServerRequest("slow/method") { request ->
            try {
                delay(5000)
                ResponseMessage(request.id, buildJsonObject { put("done", true) }, null)
            } catch (e: CancellationException) {
                jobCancelled = true
                throw e
            }
        }

        val request = RequestMessage(
            id = IntRequestId(42),
            method = "slow/method",
            params = null
        )
        val requestJson = json.encodeToString(request)
        setup.clientToServer.writeMessage(requestJson)

        delay(150)

        val cancelNotification = NotificationMessage(
            method = "$/cancelRequest",
            params = json.encodeToJsonElement(CancelParams(IntRequestId(42)))
        )
        val cancelJson = json.encodeToString(cancelNotification)
        setup.clientToServer.writeMessage(cancelJson)

        delay(300)

        jobCancelled shouldBe true

        //setup.cleanup()
    }

    test("should handle response error") {
        val setup = createTestSetup()

        val responseJob = setup.scope.launch {
            delay(150)

            val response = ResponseMessage(
                id = IntRequestId(0),
                result = null,
                error = ResponseError(
                    code = ErrorCodes.InternalError,
                    message = "Something went wrong",
                    data = null
                )
            )
            val responseJson = json.encodeToString(response)
            setup.clientToServer.writeMessage(responseJson)
        }

        val exception = shouldThrow<ResponseErrorException> {
            setup.connection.sendRequest("test/method", buildJsonObject { put("key", "value") })
        }

        exception.message shouldContain "Something went wrong"

        responseJob.cancel()
        //setup.cleanup()
    }

    test("should auto-increment request IDs") {
        val setup = createTestSetup()

        val responseJob = setup.scope.launch {
            repeat(3) { i ->
                delay(200)

                val response = ResponseMessage(
                    id = IntRequestId(i),
                    result = buildJsonObject { put("index", i) },
                    error = null
                )
                val responseJson = json.encodeToString(response)
                setup.clientToServer.writeMessage(responseJson)
            }
        }

        val results = mutableListOf<ResponseMessage>()
        repeat(3) {
            val result = setup.connection.sendRequest("test/method")
            results.add(result)
        }

        results[0].id shouldBe IntRequestId(0)
        results[1].id shouldBe IntRequestId(1)
        results[2].id shouldBe IntRequestId(2)

        responseJob.cancel()
        //setup.cleanup()
    }

    test("should handle handler errors gracefully") {
        val setup = createTestSetup()

        setup.connection.handleServerRequest("error/method") { _ ->
            throw RuntimeException("Handler error")
        }

        val request = RequestMessage(
            id = IntRequestId(1),
            method = "error/method",
            params = null
        )
        val requestJson = json.encodeToString(request)
        setup.clientToServer.writeMessage(requestJson)

        delay(300)

        val written = setup.serverToClient.readAll()
        written shouldContain "Internal error"
        written shouldContain "Handler error"
        println(written)

        //setup.cleanup()
    }

    test("should decode typed response") {
        val setup = createTestSetup()

        val responseJob = setup.scope.launch {
            delay(150)

            val response = ResponseMessage(
                id = IntRequestId(0),
                result = buildJsonObject {
                    put("name", "Test")
                    put("value", 42)
                },
                error = null
            )
            val responseJson = json.encodeToString(response)
            setup.clientToServer.writeMessage(responseJson)
        }

        val result = setup.connection.sendRequest<JsonObject, LSPAny?>("test/method", null)

        result.toString() shouldContain "\"name\":\"Test\""
        result.toString() shouldContain "\"value\":42"
        println(result)

        responseJob.cancel()
        //setup.cleanup()
    }
})

class TestStream {
    private val channel = Channel<ByteArray>(Channel.UNLIMITED)
    private val closed = atomic(false)
    private val lock = SynchronizedObject()
    private val buffer = Buffer()

    fun writeMessage(message: String) {
        if (closed.value) return

        val content = message.encodeToByteArray()
        val header = "$CONTENT_LEN_HEADER${content.size}$HEADER_DELIMITER".encodeToByteArray()
        channel.trySend(header + content)
    }

    fun close() {
        closed.update { true }
        channel.close()
    }

    fun asSource(): RawSource = object : RawSource {
        override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
            if (closed.value && buffer.size == 0L) {
                val result = channel.tryReceive()
                if (result.isFailure) return -1
                buffer.write(result.getOrNull() ?: return -1)
            }

            // Try to get data from channel if buffer is empty
            if (buffer.size == 0L) {
                val result = channel.tryReceive()
                if (result.isSuccess) {
                    result.getOrNull()?.let { buffer.write(it) }
                } else if (closed.value) {
                    return -1
                } else {
                    runBlocking {
                        withTimeoutOrNull(50) {
                            val data = channel.receive()
                            buffer.write(data)
                        }
                    }
                }
            }

            if (buffer.size == 0L) {
                return if (closed.value) -1 else 0
            }

            val toRead = minOf(buffer.size, byteCount)
            buffer.readAtMostTo(sink, toRead)
            return toRead
        }

        override fun close() {
            closed.value = true
            channel.close()
        }
    }

    fun asSink(): RawSink = object : RawSink {
        private val writeBuffer = Buffer()

        override fun write(source: Buffer, byteCount: Long) {
            synchronized(lock) {
                source.readAtMostTo(writeBuffer, byteCount)
            }
        }

        override fun flush() {
            synchronized(lock) {
                if (writeBuffer.size > 0) {
                    val bytes = ByteArray(writeBuffer.size.toInt())
                    writeBuffer.readTo(bytes)
                    channel.trySend(bytes)
                }
            }
        }

        override fun close() {
            flush()
            closed.value = true
            channel.close()
        }
    }

    suspend fun readAll(): String {
        val result = StringBuilder()

        synchronized(lock) {
            if (buffer.size > 0) {
                val bytes = ByteArray(buffer.size.toInt())
                buffer.readTo(bytes)
                result.append(bytes.decodeToString())
            }
        }

        while (true) {
            val data = channel.tryReceive().getOrNull() ?: break
            result.append(data.decodeToString())
        }

        return result.toString()
    }
}
