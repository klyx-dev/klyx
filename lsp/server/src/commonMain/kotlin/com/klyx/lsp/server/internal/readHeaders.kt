package com.klyx.lsp.server.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.Source

internal const val HEADER_DELIMITER = "\r\n\r\n"

internal suspend fun readHeaders(reader: Source, buffer: Buffer) = withContext(Dispatchers.IO) {
    val delimiter = HEADER_DELIMITER.encodeToByteArray()
    while (true) {
        if (buffer.endsWith(delimiter)) {
            return@withContext
        }

        if (reader.readUntil('\n', buffer) == 0L) {
            throw JsonRpcException("cannot read LSP message headers")
        }
    }
}

internal fun Buffer.endsWith(bytes: ByteArray): Boolean {
    if (size < bytes.size) return false

    for (i in bytes.indices) {
        if (this[size - bytes.size + i] != bytes[i]) {
            return false
        }
    }
    return true
}

internal fun Source.readUntil(char: Char, buffer: Buffer): Long = readUntil(char.code.toByte(), buffer)

internal fun Source.readUntil(delimiter: Byte, buffer: Buffer): Long {
    var count = 0L

    while (true) {
        val b = readByte()
        buffer.writeByte(b)
        count++

        if (b == delimiter) {
            break
        }
    }

    return count
}
