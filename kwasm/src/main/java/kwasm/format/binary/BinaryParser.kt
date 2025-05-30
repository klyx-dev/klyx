/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("EXPERIMENTAL_API_USAGE")

package kwasm.format.binary

import kwasm.format.ParseContext
import kwasm.format.ParseException
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Tool for reading bytes from a [reader] while supporting position tracking for error reporting
 * purposes.
 */
class BinaryParser(
    private val reader: InputStream,
    private val parseContext: ParseContext = ParseContext("unknown.wasm")
) {
    internal var position = 0
    private val byteBuffer = ByteArray(1)
    private val intBuffer = ByteArray(4)
    private val longBuffer = ByteArray(8)

    /** The last byte value read from the [reader]. */
    internal var lastByte: Byte = 0x00
        private set

    constructor(reader: InputStream, fileName: String) : this(reader, ParseContext(fileName))

    /**
     * Reads a single [Byte] from the [reader].
     *
     * From [the docs](https://webassembly.github.io/spec/core/binary/values.html#bytes):
     *
     * Bytes encode themselves.
     *
     * ```
     *      byte    ::= 0x00    => 0x00
     *                  0xFF    => 0xFF
     * ```
     */
    fun readByte(): Byte {
        val read = reader.read(byteBuffer, 0, 1)
        if (read != 1) throwException("Expected byte, but none found")
        position += read
        return byteBuffer[0].also { lastByte = it }
    }

    /** Reads a single [Byte] from the [reader], or returns `null` if the [reader] is empty. */
    fun readByteOrNull(): Byte? = try { readByte() } catch (e: ParseException) { null }

    /**
     * Reads the next four bytes from the [reader] as a little-endian-encoded [Int].
     */
    fun readFourBytes(): Int {
        val read = reader.read(intBuffer, 0, 4)
        if (read != 4) throwException("Expected 4 bytes, but $read found")
        position += read
        return intBuffer[0].toUByte().toInt() or
            (intBuffer[1].toUByte().toInt() shl 8) or
            (intBuffer[2].toUByte().toInt() shl 16) or
            (intBuffer[3].toUByte().toInt() shl 24).also { lastByte = intBuffer[3] }
    }

    /**
     * Reads the next eight bytes from the [reader] as a little-endian-encoded [Long].
     */
    fun readEightBytes(): Long {
        val read = reader.read(longBuffer, 0, 8)
        if (read != 8) throwException("Expected 8 bytes, but $read found")
        position += read
        return longBuffer[0].toUByte().toLong() or
            (longBuffer[1].toUByte().toLong() shl 8) or
            (longBuffer[2].toUByte().toLong() shl 16) or
            (longBuffer[3].toUByte().toLong() shl 24) or
            (longBuffer[4].toUByte().toLong() shl 32) or
            (longBuffer[5].toUByte().toLong() shl 40) or
            (longBuffer[6].toUByte().toLong() shl 48) or
            (longBuffer[7].toUByte().toLong() shl 56).also { lastByte = longBuffer[7] }
    }

    /** Reads [size] bytes from the [reader] into a new [ByteArray]. */
    fun readBytes(size: Int): ByteArray {
        val bytesOut = ByteArrayOutputStream(size)
        val buffer = ByteArray(4096)
        var bytesRead = 0
        while (bytesRead < size) {
            val bytes = reader.read(buffer, 0, minOf(size - bytesRead, buffer.size))
            if (bytes == -1) {
                throwException("EOF before $size bytes read ($bytesRead so far) - unexpected end")
            }
            bytesOut.write(buffer, 0, bytes)
            bytesRead += bytes
        }
        bytesOut.flush()
        position += size
        return bytesOut.toByteArray()
    }

    /**
     * Throws a [ParseException] with the given message at the current [position] plus the provided
     * [positionOffset].
     */
    fun throwException(message: String, positionOffset: Int = 0): Nothing {
        throw ParseException(message, parseContext.copy(column = position + positionOffset))
    }
}
