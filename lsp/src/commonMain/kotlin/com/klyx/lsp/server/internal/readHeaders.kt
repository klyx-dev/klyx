package com.klyx.lsp.server.internal

import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.Source

internal const val HEADER_DELIMITER = "\r\n\r\n"

internal fun readHeaders(source: Source, buffer: Buffer) {
    val delimiter = HEADER_DELIMITER.encodeToByteArray()

    while (true) {
        val read = source.readAtMostTo(buffer, 1)
        if (read == -1L) {
            if (buffer.size == 0L) {
                throw EOFException("End of stream reached")
            }
            throw EOFException("Unexpected EOF while reading headers")
        }
        if (buffer.endsWith(delimiter)) return
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
