package com.klyx.core

import java.nio.charset.Charset

fun String.byteSize(charset: Charset = detectEncoding(toByteArray()) /* = UTF-8 */): Int {
    return this.toByteArray(charset).size
}

fun detectEncoding(bytes: ByteArray): Charset {
    return when {
        bytes.startsWith(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) -> Charsets.UTF_8
        bytes.startsWith(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00)) -> Charsets.UTF_32LE
        bytes.startsWith(byteArrayOf(0x00, 0x00, 0xFE.toByte(), 0xFF.toByte())) -> Charsets.UTF_32BE
        bytes.startsWith(byteArrayOf(0xFF.toByte(), 0xFE.toByte())) -> Charsets.UTF_16LE
        bytes.startsWith(byteArrayOf(0xFE.toByte(), 0xFF.toByte())) -> Charsets.UTF_16BE
        else -> Charsets.UTF_8 // No BOM — can't be sure
    }
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (this.size < prefix.size) return false
    return this.sliceArray(prefix.indices).contentEquals(prefix)
}
