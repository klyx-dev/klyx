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

package kwasm.format.binary.type

import kwasm.ast.type.Limits
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readUInt
import kwasm.util.Leb128

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/types.html#limits):
 *
 * Limits are encoded with a preceding flag indicating whether a maximum is present.
 *
 * ```
 *      limits  ::=     0x00 n:u32          => {min n, max ϵ}
 *                      0x01 n:u32 m:u32    => {min n, max m}
 * ```
 */
fun BinaryParser.readLimits(): Limits = when (val type = readByte().toInt()) {
    0 -> Limits(readUInt().toLong(), null)
    1 -> Limits(readUInt().toLong(), readUInt().toLong())
    else ->
        throwException(
            message = "Invalid header byte for Limits declaration: $type " +
                "(integer too large|integer representation too long)",
            positionOffset = -1
        )
}

/** Encodes a [Limits] object to a sequence of bytes. */
internal fun Limits.toBytes(): Sequence<Byte> = if (max == null) {
    sequenceOf<Byte>(0x00) + Leb128.encodeUnsigned(min.toInt())
} else {
    sequenceOf<Byte>(0x01) +
        Leb128.encodeUnsigned(min.toInt()) +
        Leb128.encodeUnsigned(max.toInt())
}
