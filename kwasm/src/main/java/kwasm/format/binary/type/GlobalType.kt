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

import kwasm.ast.type.GlobalType
import kwasm.format.binary.BinaryParser

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/types.html#global-types):
 *
 * Global types are encoded by their value type and a flag for their mutability.
 *
 * ```
 *      globaltype  ::= t:valtype m:mut => m
 *      tmut        ::= 0x00            => const
 *                      0x01            => var
 * ```
 */
fun BinaryParser.readGlobalType(): GlobalType {
    val valueType = readValueType()
    return when (val mutability = readByte().toInt()) {
        0x00 -> GlobalType(valueType, false)
        0x01 -> GlobalType(valueType, true)
        else -> throwException("Invalid mutability flag: $mutability", -1)
    }
}

/** Encodes the receiving [GlobalType] to a sequence of bytes. */
internal fun GlobalType.toBytes(): Sequence<Byte> =
    sequenceOf(valueType.toByte(), if (mutable) 0x01 else 0x00)
