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

import kwasm.ast.type.ValueType
import kwasm.format.binary.BinaryParser

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/types.html#value-types):
 *
 * Value types are encoded by a single byte.
 *
 * ```
 *      valtype ::= 0x7F    =>  i32
 *                  0x7E    =>  i64
 *                  0x7D    =>  f32
 *                  0x7C    =>  f64
 * ```
 *
 * **Note**
 *
 * Value types can occur in contexts where type indices are also allowed, such as in the case of
 * block types. Thus, the binary format for types corresponds to the signed LEB128 encoding of
 * small negative `sN` values, so that they can coexist with (positive) type indices in the future.
 */
fun BinaryParser.readValueType(): ValueType {
    val type = readByte().toUByte().toInt().asValueType() ?: throwException("Value type not found", -1)
    return type
}

internal fun Int.asValueType(): ValueType? = when {
    this == 0x7F -> ValueType.I32
    this == 0x7E -> ValueType.I64
    this == 0x7D -> ValueType.F32
    this == 0x7C -> ValueType.F64
    else -> null
}

internal fun ValueType.toByte(): Byte = when (this) {
    ValueType.I32 -> 0x7F
    ValueType.I64 -> 0x7E
    ValueType.F32 -> 0x7D
    ValueType.F64 -> 0x7C
}
