package com.klyx.lsp.server.internal

import kotlinx.io.Buffer
import kotlinx.io.Source

internal const val HEADER_DELIMITER = "\r\n\r\n"

internal fun readHeaders(source: Source, buffer: Buffer) {
    val delimiter = HEADER_DELIMITER.encodeToByteArray()

    while (true) {
        source.readAtMostTo(buffer, 1)
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
