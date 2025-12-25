package com.klyx.lsp.server.internal

import com.klyx.lsp.Message
import com.klyx.lsp.NotificationMessage
import com.klyx.lsp.RequestMessage
import com.klyx.lsp.ResponseMessage
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.Sink
import kotlinx.serialization.json.Json

internal class JsonRpcWriter(val output: Sink, val json: Json) {

    private val lock = SynchronizedObject()

    suspend fun writeMessage(message: Message) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            try {
                val message = when (message) {
                    is NotificationMessage -> json.encodeToString(message)
                    is RequestMessage -> json.encodeToString(message)
                    is ResponseMessage -> json.encodeToString(message)
                }

                val content = message.encodeToByteArray()
                val header = "$CONTENT_LEN_HEADER${content.size}\r\n\r\n".encodeToByteArray()

                output.write(header)
                output.write(content)
                output.flush()
            } catch (error: Throwable) {
                throw JsonRpcException("Failed to write message: ${error.message}", error)
            }
        }
    }
}
