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

import kwasm.ast.type.ElementType
import kwasm.ast.type.TableType
import kwasm.format.binary.BinaryParser

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/types.html#table-types):
 *
 * Table types are encoded with their limits and a constant byte indicating their element type.
 *
 * ```
 *      tabletype   ::=     et:elemtype lim:limits  => lim et
 *      elemtype    ::=     0x70                    => funcref
 * ```
 */
fun BinaryParser.readTableType(): TableType = when (val byte = readByte()) {
    ELEMTYPE_FUNCREF -> TableType(readLimits(), ElementType.FunctionReference)
    else -> throwException("Illegal element type for Table: $byte", -1)
}

/** Encodes the [TableType] to a sequence of bytes. */
internal fun TableType.toBytes(): Sequence<Byte> =
    sequenceOf<Byte>(0x70) + limits.toBytes()

private const val ELEMTYPE_FUNCREF: Byte = 0x70
