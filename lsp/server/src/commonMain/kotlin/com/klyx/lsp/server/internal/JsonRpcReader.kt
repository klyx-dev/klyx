package com.klyx.lsp.server.internal

import com.klyx.lsp.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.serialization.json.Json

internal class JsonRpcReader(val input: Source, val json: Json) {
    suspend fun readMessage(): Message? = withContext(Dispatchers.IO) {
        try {
            val buffer = Buffer()
            buffer.clear()
            readHeaders(input, buffer)

            val headers = buffer.readString()
            val messageLength = headers
                .split('\n')
                .find { it.startsWith(CONTENT_LEN_HEADER) }
                ?.removePrefix(CONTENT_LEN_HEADER)
                ?.trimEnd()
                ?.toLongOrNull()
                ?: throw JsonRpcException("invalid LSP message header $headers")

            input.readAtMostTo(buffer, messageLength)
            parseMessage(buffer.readString())
        } catch (_: EOFException) {
            null
        } catch (e: Throwable) {
            throw JsonRpcException("Failed to read message: ${e.message}", e)
        }
    }

    private fun parseMessage(message: String): Message {
        return json.decodeFromString(message)
    }
}

