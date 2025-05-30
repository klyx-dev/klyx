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

package kwasm.runtime.memory

import kwasm.util.Impossible

private const val BYTE_MAX_VALUE = 256L
private const val SHORT_MAX_VALUE = 65536L
private const val INT_MAX_VALUE = 0x100000000

internal inline fun <reified T : Number> Int.assertValidByteWidth() =
    when (T::class) {
        Int::class, Float::class ->
            require(this == 1 || this == 2 || this == 4) {
                "Invalid byte width for Int/Float"
            }
        Long::class, Double::class ->
            require(this == 1 || this == 2 || this == 4 || this == 8) {
                "Invalid byte width for Long/Double"
            }
        else -> Impossible()
    }

internal fun Int.wrap(bytes: Int) = when (bytes) {
    1 -> this % BYTE_MAX_VALUE.toInt()
    2 -> this % SHORT_MAX_VALUE.toInt()
    4 -> this
    else -> Impossible()
}

internal fun Long.wrap(bytes: Int) = when (bytes) {
    1 -> this % BYTE_MAX_VALUE
    2 -> this % SHORT_MAX_VALUE
    4 -> this % INT_MAX_VALUE
    8 -> this
    else -> Impossible()
}

internal fun ULong.wrap(bytes: Int) = when (bytes) {
    1 -> this % BYTE_MAX_VALUE.toULong()
    2 -> this % SHORT_MAX_VALUE.toULong()
    4 -> this % INT_MAX_VALUE.toULong()
    8 -> this
    else -> Impossible()
}

internal fun ByteArray.toBigEndianInt(length: Int): Int {
    var result = 0
    repeat(length) { result = result or (this[it].toUByte().toInt() shl (8 * it)) }
    return result
}

internal fun ByteArray.toBigEndianLong(length: Int): Long {
    var result = 0L
    repeat(length) { result = result or (this[it].toUByte().toLong() shl (8 * it)) }
    return result
}
