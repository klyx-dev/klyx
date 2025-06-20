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

package kwasm.format.binary.value

import kwasm.format.binary.BinaryParser
import kotlin.experimental.and

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/values.html#integers):
 *
 * All integers are encoded using the LEB128 variable-length integer encoding, in either unsigned or
 * signed variant.
 *
 * Unsigned integers are encoded in unsigned LEB128 format. As an additional constraint, the total
 * number of bytes encoding a value of type uN must not exceed ceil(N/7) bytes.
 *
 * ```
 *      uN  ::= n:byte              => n                    (if n < 2^7 and n < 2^N)
 *              n:byte  m:u(N−7)    => 2^7 * m + (n - 2^7)  (if n >= 2^7 and N > 7)
 * ```
 *
 * Signed integers are encoded in signed LEB128 format, which uses a two’s complement
 * representation. As an additional constraint, the total number of bytes encoding a value of type
 * `sN` must not exceed `ceil(N/7)` bytes.
 *
 * ```
 *      sN  ::= n:byte              => n                    (if n<26 and n < 2^(N−1))
 *              n:byte              => n - 2^7              (if 26 <= n < 27 and n >= 2^7 - 2^(N−1))
 *              n:byte  m:s(N−7)    => 2^7 * m + (n - 2^7)  (if n >= 27 and N > 7)
 *
 * Uninterpreted integers are encoded as signed integers.
 *
 * ```
 *      iN  ::= n:sN                => i                    (if n = signed_iN(i))
 * ```
 *
 * **Note**
 *
 * The side conditions `N > 7` in the productions for non-terminal bytes of the `u` and `s`
 * encodings restrict the encoding’s length. However, “trailing zeros” are still allowed within
 * these bounds. For example, `0x03` and `0x83 0x00` are both well-formed encodings for the value
 * `3` as a u8. Similarly, either of `0x7e` and `0xFE 0x7F` and `0xFE 0xFF 0x7F` are well-formed
 * encodings of the value `−2` as a `s16`.
 *
 * The side conditions on the value `n` of terminal bytes further enforce that any unused bits in
 * these bytes must be `0` for positive values and 1 for negative ones. For example, `0x83 0x10` is
 * malformed as a `u8` encoding. Similarly, both `0x83 0x3E` and `0xFF 0x7B` are malformed as s8
 * encodings.
 */
fun BinaryParser.readUInt(): Int {
    var result = 0
    var shift = 0
    var bytesRemaining = 5
    while (true) {
        val byte = readByte()
        bytesRemaining--
        if (bytesRemaining < 0) {
            throwException("Bad LEB-128 encoding: integer representation too long")
        } else if (bytesRemaining == 0 && (byte and FIRST_SEVEN).toUInt() shr 4 > 0.toUInt()) {
            throwException("Bad LEB-128 encoding: integer too large")
        }

        result = result or ((byte and FIRST_SEVEN).toInt() shl shift)
        shift += 7
        if (byte and LEADING_ONE == ZERO_BYTE) break
    }
    return result
}

/**
 * See [BinaryParser.readUInt].
 */
fun BinaryParser.readInt(): Int {
    var result = 0
    var shift = 0
    var bytesRemaining = 5
    while (true) {
        val byte = readByte()
        bytesRemaining--
        if (bytesRemaining < 0) {
            throwException("Bad LEB-128 encoding: integer representation too long")
        }

        result = result or ((byte and FIRST_SEVEN).toInt() shl shift)
        shift += 7
        if (byte and LEADING_ONE == ZERO_BYTE) {
            if (shift < 32 && (byte and 0x40) != ZERO_BYTE) {
                result = result or (ONES_INT shl shift)
            } else if (shift >= 32) {
                val leftFour = byte.toUInt() shr 4
                if (leftFour > 0.toUInt() && byte and 0x7.toByte() == 0.toByte()) {
                    throwException("Bad LEB-128 encoding: integer too large")
                } else if (leftFour != 0x7.toUInt() && byte and 0x7.toByte() == 0x7.toByte()) {
                    throwException("Bad LEB-128 encoding: integer too large")
                }
            }
            break
        }
    }
    return result
}

/**
 * See [BinaryParser.readUInt].
 */
fun BinaryParser.readULong(): Long {
    var result = 0uL
    var shift = 0
    var bytesRemaining = 10
    while (true) {
        val byte = readByte()
        bytesRemaining--
        if (bytesRemaining < 0) {
            throwException("Bad LEB-128 encoding: integer representation too long")
        } else if (bytesRemaining == 0 && (byte and FIRST_SEVEN).toULong() shr 1 > 0.toULong()) {
            throwException("Bad LEB-128 encoding: integer too large")
        }

        result = result or ((byte and FIRST_SEVEN).toULong() shl shift)
        shift += 7
        if (byte and LEADING_ONE == ZERO_BYTE) break
    }
    return result.toLong()
}

/**
 * See [BinaryParser.readUInt].
 */
fun BinaryParser.readLong(): Long {
    var result = 0L
    var shift = 0
    var bytesRemaining = 10
    while (true) {
        val byte = readByte()
        bytesRemaining--
        if (bytesRemaining < 0) {
            throwException("Bad LEB-128 encoding: integer representation too long")
        }

        result = result or ((byte and FIRST_SEVEN).toLong() shl shift)
        shift += 7
        if (byte and LEADING_ONE == ZERO_BYTE) {
            if (shift < 64 && (byte and 0x40) != ZERO_BYTE) {
                result = result or (ONES_LONG shl shift)
            } else if (shift >= 64) {
                val leftSeven = byte.toULong() shr 1
                if (leftSeven > 0.toULong() && byte and 0x1.toByte() == 0.toByte()) {
                    throwException("Bad LEB-128 encoding: integer too large")
                } else if (leftSeven != 0x3F.toULong() && byte and 0x1.toByte() == 0x1.toByte()) {
                    throwException("Bad LEB-128 encoding: integer too large")
                }
            }
            break
        }
    }
    return result
}

private const val ZERO_BYTE = 0.toByte()
private const val ONES_INT = -1
private const val ONES_LONG = -1L
private const val LEADING_ONE = 0x80.toByte()
private const val FIRST_SEVEN = 0x7F.toByte()
