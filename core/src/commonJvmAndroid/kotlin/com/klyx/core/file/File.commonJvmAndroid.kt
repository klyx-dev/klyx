package com.klyx.core.file

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

actual fun ByteArray.isValidUtf8(): Boolean {
    val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)

    return try {
        decoder.decode(ByteBuffer.wrap(this))
        true
    } catch (_: Exception) {
        false
    }
}
