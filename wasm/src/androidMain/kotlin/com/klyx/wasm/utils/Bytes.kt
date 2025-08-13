package com.klyx.wasm.utils

/**
 * Reads a little endian 16-bit signed integer from the byte array at the specified offset
 */
fun ByteArray.readInt16LE(offset: Int = 0): Short {
    require(offset + 1 < size) { "Buffer overflow: offset $offset, size $size" }
    return ((this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8)).toShort()
}

/**
 * Reads a little endian 16-bit unsigned integer from the byte array at the specified offset
 */
fun ByteArray.readUInt16LE(offset: Int = 0): UShort {
    require(offset + 1 < size) { "Buffer overflow: offset $offset, size $size" }
    return ((this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8)).toUShort()
}

/**
 * Reads a little endian 32-bit signed integer from the byte array at the specified offset
 */
fun ByteArray.readInt32LE(offset: Int = 0): Int {
    require(offset + 3 < size) { "Buffer overflow: offset $offset, size $size" }
    return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)
}

/**
 * Reads a little endian 32-bit unsigned integer from the byte array at the specified offset
 */
fun ByteArray.readUInt32LE(offset: Int = 0): UInt {
    require(offset + 3 < size) { "Buffer overflow: offset $offset, size $size" }
    return ((this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)).toUInt()
}

/**
 * Reads a little endian 64-bit signed integer from the byte array at the specified offset
 */
fun ByteArray.readInt64LE(offset: Int = 0): Long {
    require(offset + 7 < size) { "Buffer overflow: offset $offset, size $size" }
    return (this[offset].toLong() and 0xFF) or
            ((this[offset + 1].toLong() and 0xFF) shl 8) or
            ((this[offset + 2].toLong() and 0xFF) shl 16) or
            ((this[offset + 3].toLong() and 0xFF) shl 24) or
            ((this[offset + 4].toLong() and 0xFF) shl 32) or
            ((this[offset + 5].toLong() and 0xFF) shl 40) or
            ((this[offset + 6].toLong() and 0xFF) shl 48) or
            ((this[offset + 7].toLong() and 0xFF) shl 56)
}

/**
 * Reads a little endian 64-bit unsigned integer from the byte array at the specified offset
 */
fun ByteArray.readUInt64LE(offset: Int = 0): ULong {
    require(offset + 7 < size) { "Buffer overflow: offset $offset, size $size" }
    return ((this[offset].toLong() and 0xFF) or
            ((this[offset + 1].toLong() and 0xFF) shl 8) or
            ((this[offset + 2].toLong() and 0xFF) shl 16) or
            ((this[offset + 3].toLong() and 0xFF) shl 24) or
            ((this[offset + 4].toLong() and 0xFF) shl 32) or
            ((this[offset + 5].toLong() and 0xFF) shl 40) or
            ((this[offset + 6].toLong() and 0xFF) shl 48) or
            ((this[offset + 7].toLong() and 0xFF) shl 56)).toULong()
}

/**
 * Reads a little endian 32-bit float from the byte array at the specified offset
 */
fun ByteArray.readFloatLE(offset: Int = 0): Float {
    return Float.fromBits(readInt32LE(offset))
}

/**
 * Reads a little endian 64-bit double from the byte array at the specified offset
 */
fun ByteArray.readDoubleLE(offset: Int = 0): Double {
    return Double.fromBits(readInt64LE(offset))
}

/**
 * Writes a little endian 16-bit signed integer to the byte array at the specified offset
 */
fun ByteArray.writeInt16LE(value: Short, offset: Int = 0) {
    require(offset + 1 < size) { "Buffer overflow: offset $offset, size $size" }
    this[offset] = (value.toInt() and 0xFF).toByte()
    this[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
}

/**
 * Writes a little endian 16-bit unsigned integer to the byte array at the specified offset
 */
fun ByteArray.writeUInt16LE(value: UShort, offset: Int = 0) {
    require(offset + 1 < size) { "Buffer overflow: offset $offset, size $size" }
    this[offset] = (value.toInt() and 0xFF).toByte()
    this[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
}

/**
 * Writes a little endian 32-bit signed integer to the byte array at the specified offset
 */
fun ByteArray.writeInt32LE(value: Int, offset: Int = 0) {
    require(offset + 3 < size) { "Buffer overflow: offset $offset, size $size" }
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = ((value shr 8) and 0xFF).toByte()
    this[offset + 2] = ((value shr 16) and 0xFF).toByte()
    this[offset + 3] = ((value shr 24) and 0xFF).toByte()
}

/**
 * Writes a little endian 32-bit unsigned integer to the byte array at the specified offset
 */
fun ByteArray.writeUInt32LE(value: UInt, offset: Int = 0) {
    require(offset + 3 < size) { "Buffer overflow: offset $offset, size $size" }
    val intValue = value.toInt()
    this[offset] = (intValue and 0xFF).toByte()
    this[offset + 1] = ((intValue shr 8) and 0xFF).toByte()
    this[offset + 2] = ((intValue shr 16) and 0xFF).toByte()
    this[offset + 3] = ((intValue shr 24) and 0xFF).toByte()
}

/**
 * Writes a little endian 64-bit signed integer to the byte array at the specified offset
 */
fun ByteArray.writeInt64LE(value: Long, offset: Int = 0) {
    require(offset + 7 < size) { "Buffer overflow: offset $offset, size $size" }
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = ((value shr 8) and 0xFF).toByte()
    this[offset + 2] = ((value shr 16) and 0xFF).toByte()
    this[offset + 3] = ((value shr 24) and 0xFF).toByte()
    this[offset + 4] = ((value shr 32) and 0xFF).toByte()
    this[offset + 5] = ((value shr 40) and 0xFF).toByte()
    this[offset + 6] = ((value shr 48) and 0xFF).toByte()
    this[offset + 7] = ((value shr 56) and 0xFF).toByte()
}

/**
 * Writes a little endian 64-bit unsigned integer to the byte array at the specified offset
 */
fun ByteArray.writeUInt64LE(value: ULong, offset: Int = 0) {
    require(offset + 7 < size) { "Buffer overflow: offset $offset, size $size" }
    val longValue = value.toLong()
    this[offset] = (longValue and 0xFF).toByte()
    this[offset + 1] = ((longValue shr 8) and 0xFF).toByte()
    this[offset + 2] = ((longValue shr 16) and 0xFF).toByte()
    this[offset + 3] = ((longValue shr 24) and 0xFF).toByte()
    this[offset + 4] = ((longValue shr 32) and 0xFF).toByte()
    this[offset + 5] = ((longValue shr 40) and 0xFF).toByte()
    this[offset + 6] = ((longValue shr 48) and 0xFF).toByte()
    this[offset + 7] = ((longValue shr 56) and 0xFF).toByte()
}

/**
 * Writes a little endian 32-bit float to the byte array at the specified offset
 */
fun ByteArray.writeFloatLE(value: Float, offset: Int = 0) {
    writeInt32LE(value.toRawBits(), offset)
}

/**
 * Writes a little endian 64-bit double to the byte array at the specified offset
 */
fun ByteArray.writeDoubleLE(value: Double, offset: Int = 0) {
    writeInt64LE(value.toRawBits(), offset)
}

/**
 * Converts a Short to little endian byte array
 */
fun Short.toLittleEndianBytes(): ByteArray {
    return ByteArray(2).apply {
        writeInt16LE(this@toLittleEndianBytes, 0)
    }
}

/**
 * Converts a UShort to little endian byte array
 */
fun UShort.toLittleEndianBytes(): ByteArray {
    return ByteArray(2).apply {
        writeUInt16LE(this@toLittleEndianBytes, 0)
    }
}

/**
 * Converts an Int to little endian byte array
 */
fun Int.toLittleEndianBytes(): ByteArray {
    return ByteArray(4).apply {
        writeInt32LE(this@toLittleEndianBytes, 0)
    }
}

/**
 * Converts a UInt to little endian byte array
 */
fun UInt.toLittleEndianBytes(): ByteArray {
    return ByteArray(4).apply {
        writeUInt32LE(this@toLittleEndianBytes, 0)
    }
}

/**
 * Converts a Long to little endian byte array
 */
fun Long.toLittleEndianBytes(): ByteArray {
    return ByteArray(8).apply {
        writeInt64LE(this@toLittleEndianBytes, 0)
    }
}

/**
 * Converts a ULong to little endian byte array
 */
fun ULong.toLittleEndianBytes(): ByteArray {
    return ByteArray(8).apply {
        writeUInt64LE(this@toLittleEndianBytes, 0)
    }
}

/**
 * Converts a Float to little endian byte array
 */
fun Float.toLittleEndianBytes(): ByteArray {
    return ByteArray(4).apply {
        writeFloatLE(this@toLittleEndianBytes, 0)
    }
}

/**
 * Converts a Double to little endian byte array
 */
fun Double.toLittleEndianBytes(): ByteArray {
    return ByteArray(8).apply {
        writeDoubleLE(this@toLittleEndianBytes, 0)
    }
}

/**
 * Reads a null-terminated UTF-8 string from the byte array at the specified offset
 */
fun ByteArray.readNullTerminatedStringLE(offset: Int = 0): String {
    require(offset < size) { "Buffer overflow: offset $offset, size $size" }

    val endIndex = (offset until size).find { this[it] == 0.toByte() } ?: size
    return sliceArray(offset until endIndex).toString(Charsets.UTF_8)
}

/**
 * Reads a UTF-8 string of specified length from the byte array at the specified offset
 */
fun ByteArray.readStringLE(offset: Int = 0, length: Int): String {
    require(offset + length <= size) { "Buffer overflow: offset $offset, length $length, size $size" }
    return sliceArray(offset until offset + length).toString(Charsets.UTF_8)
}

/**
 * Reads a UTF-16LE string from the byte array at the specified offset
 */
fun ByteArray.readString16LE(offset: Int = 0, charCount: Int): String {
    require(offset + charCount * 2 <= size) { "Buffer overflow: offset $offset, chars $charCount, size $size" }

    val chars = CharArray(charCount)
    for (i in 0 until charCount) {
        chars[i] = readUInt16LE(offset + i * 2).toInt().toChar()
    }
    return String(chars)
}

/**
 * Reads a length-prefixed UTF-8 string (32-bit length prefix) from the byte array
 */
fun ByteArray.readLengthPrefixedStringLE(offset: Int = 0): String {
    val length = readInt32LE(offset)
    return readStringLE(offset + 4, length)
}

/**
 * Writes a null-terminated UTF-8 string to the byte array at the specified offset
 * Returns the number of bytes written (including null terminator)
 */
fun ByteArray.writeNullTerminatedStringLE(value: String, offset: Int = 0): Int {
    val stringBytes = value.toByteArray(Charsets.UTF_8)
    require(offset + stringBytes.size + 1 <= size) {
        "Buffer overflow: offset $offset, string length ${stringBytes.size}, size $size"
    }

    stringBytes.copyInto(this, offset)
    this[offset + stringBytes.size] = 0 // null terminator
    return stringBytes.size + 1
}

/**
 * Writes a UTF-8 string to the byte array at the specified offset
 * Returns the number of bytes written
 */
fun ByteArray.writeStringLE(value: String, offset: Int = 0): Int {
    val stringBytes = value.toByteArray(Charsets.UTF_8)
    require(offset + stringBytes.size <= size) {
        "Buffer overflow: offset $offset, string length ${stringBytes.size}, size $size"
    }

    stringBytes.copyInto(this, offset)
    return stringBytes.size
}

/**
 * Writes a UTF-16LE string to the byte array at the specified offset
 * Returns the number of bytes written
 */
fun ByteArray.writeString16LE(value: String, offset: Int = 0): Int {
    require(offset + value.length * 2 <= size) {
        "Buffer overflow: offset $offset, string length ${value.length}, size $size"
    }

    for (i in value.indices) {
        writeUInt16LE(value[i].code.toUShort(), offset + i * 2)
    }
    return value.length * 2
}

/**
 * Writes a length-prefixed UTF-8 string (32-bit length prefix) to the byte array
 * Returns the number of bytes written (including length prefix)
 */
fun ByteArray.writeLengthPrefixedStringLE(value: String, offset: Int = 0): Int {
    val stringBytes = value.toByteArray(Charsets.UTF_8)
    require(offset + 4 + stringBytes.size <= size) {
        "Buffer overflow: offset $offset, total length ${4 + stringBytes.size}, size $size"
    }

    writeInt32LE(stringBytes.size, offset)
    stringBytes.copyInto(this, offset + 4)
    return 4 + stringBytes.size
}

/**
 * Converts a String to UTF-8 bytes with null terminator
 */
fun String.toNullTerminatedBytesLE(): ByteArray {
    val stringBytes = toByteArray(Charsets.UTF_8)
    return ByteArray(stringBytes.size + 1).apply {
        stringBytes.copyInto(this, 0)
        this[stringBytes.size] = 0
    }
}

/**
 * Converts a String to UTF-8 bytes
 */
fun String.toBytesLE(): ByteArray = toByteArray(Charsets.UTF_8)

/**
 * Converts a String to UTF-16LE bytes
 */
fun String.toBytes16LE(): ByteArray {
    return ByteArray(length * 2).apply {
        for (i in indices) {
            writeUInt16LE(this@toBytes16LE[i].code.toUShort(), i * 2)
        }
    }
}

/**
 * Converts a String to length-prefixed UTF-8 bytes (32-bit length prefix)
 */
fun String.toLengthPrefixedBytesLE(): ByteArray {
    val stringBytes = toByteArray(Charsets.UTF_8)
    return ByteArray(4 + stringBytes.size).apply {
        writeInt32LE(stringBytes.size, 0)
        stringBytes.copyInto(this, 4)
    }
}
