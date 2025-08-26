package com.klyx.wasm.internal

/**
 * Reads a 16-bit signed integer in little-endian format
 */
fun ByteArray.readInt16LittleEndian(offset: Int = 0): Short {
    require(offset + 1 < size) { "Buffer overflow: offset $offset, size $size" }
    return ((this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8)).toShort()
}

/**
 * Reads a 16-bit unsigned integer in little-endian format
 */
fun ByteArray.readUInt16LittleEndian(offset: Int = 0): UShort {
    require(offset + 1 < size) { "Buffer overflow: offset $offset, size $size" }
    return ((this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8)).toUShort()
}

/**
 * Reads a 32-bit signed integer in little-endian format
 */
fun ByteArray.readInt32LittleEndian(offset: Int = 0): Int {
    require(offset + 3 < size) { "Buffer overflow: offset $offset, size $size" }
    return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)
}

/**
 * Reads a 32-bit unsigned integer in little-endian format
 */
fun ByteArray.readUInt32LittleEndian(offset: Int = 0): UInt {
    require(offset + 3 < size) { "Buffer overflow: offset $offset, size $size" }
    return ((this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)).toUInt()
}

/**
 * Reads a 64-bit signed integer in little-endian format
 */
fun ByteArray.readInt64LittleEndian(offset: Int = 0): Long {
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
 * Reads a 64-bit unsigned integer in little-endian format
 */
fun ByteArray.readUInt64LittleEndian(offset: Int = 0): ULong {
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
 * Reads a 32-bit float in little-endian format
 */
fun ByteArray.readFloat32LittleEndian(offset: Int = 0): Float {
    return Float.fromBits(readInt32LittleEndian(offset))
}

/**
 * Reads a 64-bit double in little-endian format
 */
fun ByteArray.readFloat64LittleEndian(offset: Int = 0): Double {
    return Double.fromBits(readInt64LittleEndian(offset))
}

/**
 * Writes a 16-bit signed integer in little-endian format
 */
fun ByteArray.writeInt16LittleEndian(value: Short, offset: Int = 0) {
    require(offset + 1 < size) { "Buffer overflow: offset $offset, size $size" }
    this[offset] = (value.toInt() and 0xFF).toByte()
    this[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
}

/**
 * Writes a 16-bit unsigned integer in little-endian format
 */
fun ByteArray.writeUInt16LittleEndian(value: UShort, offset: Int = 0) {
    require(offset + 1 < size) { "Buffer overflow: offset $offset, size $size" }
    this[offset] = (value.toInt() and 0xFF).toByte()
    this[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
}

/**
 * Writes a 32-bit signed integer in little-endian format
 */
fun ByteArray.writeInt32LittleEndian(value: Int, offset: Int = 0) {
    require(offset + 3 < size) { "Buffer overflow: offset $offset, size $size" }
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = ((value shr 8) and 0xFF).toByte()
    this[offset + 2] = ((value shr 16) and 0xFF).toByte()
    this[offset + 3] = ((value shr 24) and 0xFF).toByte()
}

/**
 * Writes a 32-bit unsigned integer in little-endian format
 */
fun ByteArray.writeUInt32LittleEndian(value: UInt, offset: Int = 0) {
    require(offset + 3 < size) { "Buffer overflow: offset $offset, size $size" }
    val intValue = value.toInt()
    this[offset] = (intValue and 0xFF).toByte()
    this[offset + 1] = ((intValue shr 8) and 0xFF).toByte()
    this[offset + 2] = ((intValue shr 16) and 0xFF).toByte()
    this[offset + 3] = ((intValue shr 24) and 0xFF).toByte()
}

/**
 * Writes a 64-bit signed integer in little-endian format
 */
fun ByteArray.writeInt64LittleEndian(value: Long, offset: Int = 0) {
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
 * Writes a 64-bit unsigned integer in little-endian format
 */
fun ByteArray.writeUInt64LittleEndian(value: ULong, offset: Int = 0) {
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
 * Writes a 32-bit float in little-endian format
 */
fun ByteArray.writeFloat32LittleEndian(value: Float, offset: Int = 0) {
    writeInt32LittleEndian(value.toRawBits(), offset)
}

/**
 * Writes a 64-bit double in little-endian format
 */
fun ByteArray.writeFloat64LittleEndian(value: Double, offset: Int = 0) {
    writeInt64LittleEndian(value.toRawBits(), offset)
}

/**
 * Converts a 16-bit signed integer to little-endian byte array
 */
fun Short.toLittleEndianByteArray(): ByteArray {
    return ByteArray(2).apply {
        writeInt16LittleEndian(this@toLittleEndianByteArray, 0)
    }
}

/**
 * Converts a 16-bit unsigned integer to little-endian byte array
 */
fun UShort.toLittleEndianByteArray(): ByteArray {
    return ByteArray(2).apply {
        writeUInt16LittleEndian(this@toLittleEndianByteArray, 0)
    }
}

/**
 * Converts a 32-bit signed integer to little-endian byte array
 */
fun Int.toLittleEndianByteArray(): ByteArray {
    return ByteArray(4).apply {
        writeInt32LittleEndian(this@toLittleEndianByteArray, 0)
    }
}

/**
 * Converts a 32-bit unsigned integer to little-endian byte array
 */
fun UInt.toLittleEndianByteArray(): ByteArray {
    return ByteArray(4).apply {
        writeUInt32LittleEndian(this@toLittleEndianByteArray, 0)
    }
}

/**
 * Converts a 64-bit signed integer to little-endian byte array
 */
fun Long.toLittleEndianByteArray(): ByteArray {
    return ByteArray(8).apply {
        writeInt64LittleEndian(this@toLittleEndianByteArray, 0)
    }
}

/**
 * Converts a 64-bit unsigned integer to little-endian byte array
 */
fun ULong.toLittleEndianByteArray(): ByteArray {
    return ByteArray(8).apply {
        writeUInt64LittleEndian(this@toLittleEndianByteArray, 0)
    }
}

/**
 * Converts a 32-bit float to little-endian byte array
 */
fun Float.toLittleEndianByteArray(): ByteArray {
    return ByteArray(4).apply {
        writeFloat32LittleEndian(this@toLittleEndianByteArray, 0)
    }
}

/**
 * Converts a 64-bit double to little-endian byte array
 */
fun Double.toLittleEndianByteArray(): ByteArray {
    return ByteArray(8).apply {
        writeFloat64LittleEndian(this@toLittleEndianByteArray, 0)
    }
}

/**
 * Reads a null-terminated UTF-8 string from the byte array
 */
fun ByteArray.readNullTerminatedUtf8String(offset: Int = 0): String {
    require(offset < size) { "Buffer overflow: offset $offset, size $size" }

    val endIndex = (offset until size).find { this[it] == 0.toByte() } ?: size
    return sliceArray(offset until endIndex).decodeToString()
}

/**
 * Reads a UTF-8 string of specified byte length from the byte array
 */
fun ByteArray.readUtf8String(offset: Int = 0, byteLength: Int): String {
    require(offset + byteLength <= size) { "Buffer overflow: offset $offset, length $byteLength, size $size" }
    return sliceArray(offset until offset + byteLength).decodeToString()
}

/**
 * Reads a UTF-16 string in little-endian format from the byte array
 */
fun ByteArray.readUtf16StringLittleEndian(offset: Int = 0, characterCount: Int): String {
    require(offset + characterCount * 2 <= size) { "Buffer overflow: offset $offset, chars $characterCount, size $size" }

    val chars = CharArray(characterCount)
    for (i in 0 until characterCount) {
        chars[i] = readUInt16LittleEndian(offset + i * 2).toInt().toChar()
    }
    return chars.concatToString()
}

/**
 * Reads a length-prefixed UTF-8 string (32-bit length prefix in little-endian)
 */
fun ByteArray.readLengthPrefixedUtf8String(offset: Int = 0): String {
    val length = readInt32LittleEndian(offset)
    return readUtf8String(offset + 4, length)
}

/**
 * Writes a null-terminated UTF-8 string to the byte array
 * Returns the number of bytes written (including null terminator)
 */
fun ByteArray.writeNullTerminatedUtf8String(value: String, offset: Int = 0): Int {
    val stringBytes = value.encodeToByteArray()
    require(offset + stringBytes.size + 1 <= size) {
        "Buffer overflow: offset $offset, string length ${stringBytes.size}, size $size"
    }

    stringBytes.copyInto(this, offset)
    this[offset + stringBytes.size] = 0 // null terminator
    return stringBytes.size + 1
}

/**
 * Writes a UTF-8 string to the byte array
 * Returns the number of bytes written
 */
fun ByteArray.writeUtf8String(value: String, offset: Int = 0): Int {
    val stringBytes = value.encodeToByteArray()
    require(offset + stringBytes.size <= size) {
        "Buffer overflow: offset $offset, string length ${stringBytes.size}, size $size"
    }

    stringBytes.copyInto(this, offset)
    return stringBytes.size
}

/**
 * Writes a UTF-16 string in little-endian format to the byte array
 * Returns the number of bytes written
 */
fun ByteArray.writeUtf16StringLittleEndian(value: String, offset: Int = 0): Int {
    require(offset + value.length * 2 <= size) {
        "Buffer overflow: offset $offset, string length ${value.length}, size $size"
    }

    for (i in value.indices) {
        writeUInt16LittleEndian(value[i].code.toUShort(), offset + i * 2)
    }
    return value.length * 2
}

/**
 * Writes a length-prefixed UTF-8 string (32-bit length prefix in little-endian)
 * Returns the number of bytes written (including length prefix)
 */
fun ByteArray.writeLengthPrefixedUtf8String(value: String, offset: Int = 0): Int {
    val stringBytes = value.encodeToByteArray()
    require(offset + 4 + stringBytes.size <= size) {
        "Buffer overflow: offset $offset, total length ${4 + stringBytes.size}, size $size"
    }

    writeInt32LittleEndian(stringBytes.size, offset)
    stringBytes.copyInto(this, offset + 4)
    return 4 + stringBytes.size
}

/**
 * Converts a string to UTF-8 bytes with null terminator
 */
fun String.toNullTerminatedUtf8ByteArray(): ByteArray {
    val stringBytes = encodeToByteArray()
    return ByteArray(stringBytes.size + 1).apply {
        stringBytes.copyInto(this, 0)
        this[stringBytes.size] = 0
    }
}

/**
 * Converts a string to UTF-8 bytes
 */
fun String.toUtf8ByteArray(): ByteArray = encodeToByteArray()

/**
 * Converts a string to UTF-16 bytes in little-endian format
 */
fun String.toUtf16ByteArrayLittleEndian(): ByteArray {
    return ByteArray(length * 2).apply {
        for (i in indices) {
            writeUInt16LittleEndian(this@toUtf16ByteArrayLittleEndian[i].code.toUShort(), i * 2)
        }
    }
}

/**
 * Converts a string to length-prefixed UTF-8 bytes (32-bit length prefix in little-endian)
 */
fun String.toLengthPrefixedUtf8ByteArray(): ByteArray {
    val stringBytes = encodeToByteArray()
    return ByteArray(4 + stringBytes.size).apply {
        writeInt32LittleEndian(stringBytes.size, 0)
        stringBytes.copyInto(this, 4)
    }
}

/**
 * Converts a string to unsigned UTF-8 byte array
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun String.toUtf8UByteArray(): UByteArray = toUtf8ByteArray().toUByteArray()
