package com.klyx.core.file

actual fun KxFile.hash(algorithm: String): String {
    TODO("Not yet implemented")
}

actual fun KxFile.isBinaryEqualTo(other: KxFile): Boolean {
    TODO("Not yet implemented")
}

actual fun ByteArray.isValidUtf8(): Boolean {
    var i = 0
    while (i < size) {
        val byte = this[i].toInt() and 0xFF
        val length = when {
            byte and 0x80 == 0x00 -> 1 // 0xxxxxxx (ASCII)
            byte and 0xE0 == 0xC0 -> 2 // 110xxxxx
            byte and 0xF0 == 0xE0 -> 3 // 1110xxxx
            byte and 0xF8 == 0xF0 -> 4 // 11110xxx
            else -> return false // Invalid leading byte
        }

        if (i + length > size) return false // Not enough bytes

        for (j in 1 until length) {
            val nextByte = this[i + j].toInt() and 0xFF
            if (nextByte and 0xC0 != 0x80) return false // Not a continuation byte
        }

        i += length
    }

    return true
}
