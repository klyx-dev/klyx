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

package kwasm.format.binary.value

import kwasm.format.binary.BinaryParser
import kwasm.util.Leb128

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/conventions.html#vectors):
 *
 * Vectors are encoded with their u32 length followed by the encoding of their element sequence.
 *
 * ```
 *      vec(B)  ::= n:u32 (x:B)n   =>  x^n
 * ```
 */
fun BinaryParser.readVector(): List<Byte> = readVector { readByte() }

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/conventions.html#vectors):
 *
 * Vectors are encoded with their u32 length followed by the encoding of their element sequence.
 *
 * ```
 *      vec(B)  ::= n:u32 (x:B)n   =>  x^n
 * ```
 */
@Suppress("EXPERIMENTAL_API_USAGE")
fun <T> BinaryParser.readVector(block: BinaryParser.() -> T): List<T> {
    val size = readUInt()
    if (size >= Int.MAX_VALUE.toLong() || size < 0) throwException("vector too long")
    return (0 until size).map { block() }
}

/**
 * Encodes the receiving [List] into a sequence of bytes, using [transform] to convert each element
 * into a sub sequence.
 */
internal fun <T> List<T>.toBytesAsVector(transform: (T) -> Sequence<Byte>): Sequence<Byte> =
    Leb128.encodeUnsigned(size) + sequence {
        forEach { item ->
            transform(item).forEach { b -> yield(b) }
        }
    }

/** Encodes the receiving [ByteArray] into a sequence of bytes. */
internal fun ByteArray.toBytesAsVector(): Sequence<Byte> =
    Leb128.encodeUnsigned(size) + sequence { forEach { yield(it) } }
