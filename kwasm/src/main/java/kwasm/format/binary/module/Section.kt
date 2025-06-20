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

import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readUInt

/** Represents a section of a binary-encoded webassembly module. */
interface Section

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#sections):
 *
 * Each section consists of
 *
 * * a one-byte section id,
 * * the`u32` size of the contents, in bytes,
 * * the actual contents, whose structure is depended on the section id.
 *
 * Every section is optional; an omitted section is equivalent to the section being present with
 * empty contents.
 *
 * The following parameterized grammar rule defines the generic structure of a section with id `N`
 * and contents described by the grammar `B`.
 *
 * ```
 *  section_N(B)    ::= N:byte size:u32 cont:B  => cont (if size = ||B||)
 *                      ϵ                       => ϵ
 * ```
 *
 * *Note*
 * Other than for unknown custom sections, the `size` is not required for decoding, but can be used
 * to skip sections when navigating through a binary. The module is malformed if the size does not
 * match the length of the binary contents `B`.
 */
fun BinaryParser.readSection(): Section? {
    val id = readByteOrNull() ?: return null
    try {
        val size = readUInt()
        val positionBefore = position
        val section = when (id) {
            0.toByte() -> readCustomSection(size)
            1.toByte() -> readTypeSection()
            2.toByte() -> readImportSection()
            3.toByte() -> readFunctionSection()
            4.toByte() -> readTableSection()
            5.toByte() -> readMemorySection()
            6.toByte() -> readGlobalSection()
            7.toByte() -> readExportSection()
            8.toByte() -> readStartSection()
            9.toByte() -> readElementSection()
            10.toByte() -> readCodeSection()
            11.toByte() -> readDataSection()
            else -> throwException("Invalid section ID: $id (malformed section id)", -5)
        }
        val consumed = position - positionBefore
        if (consumed != size) {
            throwException(
                "Invalid section size. Expected $size bytes, consumed $consumed " +
                    "(section size mismatch)",
                -consumed - 5
            )
        }
        return section
    } catch (e: ParseException) {
        if ("Expected byte" in (e.message ?: "")) {
            throwException(
                e.message + " (unexpected end of section or function|length out of bounds)"
            )
        }
        throw e
    }
}
