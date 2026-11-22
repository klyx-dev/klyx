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

internal class JsonRpcReader(
    private val input: Source,
    private val json: Json
) {
    suspend fun readMessage(): Message? = withContext(Dispatchers.IO) {
        try {
            val headerBuffer = Buffer()
            readHeaders(input, headerBuffer)

            val headers = headerBuffer.readString()
            val contentLength = parseContentLength(headers)

            val bodyBuffer = Buffer()
            readFully(input, bodyBuffer, contentLength)

            json.decodeFromString(bodyBuffer.readString())
        } catch (_: EOFException) {
            null
        } catch (e: Throwable) {
            throw JsonRpcException("Failed to read message: ${e.message}", e)
        }
    }

    private fun parseContentLength(headers: String): Long {
        return headers
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(CONTENT_LEN_HEADER, ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.toLongOrNull()
            ?: throw JsonRpcException("invalid LSP message header $headers")
    }
}

private fun readFully(source: Source, buffer: Buffer, byteCount: Long) {
    var remaining = byteCount
    while (remaining > 0) {
        val read = source.readAtMostTo(buffer, remaining)
        if (read == -1L) {
            throw EOFException("Unexpected EOF while reading LSP message body")
        }
        remaining -= read
    }
}
