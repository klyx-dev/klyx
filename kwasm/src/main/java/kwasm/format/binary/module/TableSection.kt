/*
 * Copyright 2021 Google LLC
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

package kwasm.format.binary.module

import kwasm.ast.type.TableType
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.type.readTableType
import kwasm.format.binary.type.toBytes
import kwasm.format.binary.value.readVector
import kwasm.format.binary.value.toBytesAsVector
import kwasm.util.Leb128

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#table-section):
 *
 * The table section has the id 4. It decodes into a vector of tables that represent the `tables`
 * component of a module.
 *
 * ```
 *      tablesec    ::= tab*:section_4(vec(table))  => tab*
 *      table       ::= tt:tabletype                => {type tt}
 * ```
 */
fun BinaryParser.readTableSection(): TableSection = TableSection(readVector { readTableType() })

/** Encodes a [TableSection] into a sequence of Bytes. */
internal fun TableSection.toBytesNoHeader(): Sequence<Byte> {
    val vecBytes = tables.toBytesAsVector { it.toBytes() }.toList()
    return Leb128.encodeUnsigned(vecBytes.size) + vecBytes.asSequence()
}

/** Represents a binary-encoding of the tables declared by a WebAssembly module. */
data class TableSection(val tables: List<TableType>) : Section
