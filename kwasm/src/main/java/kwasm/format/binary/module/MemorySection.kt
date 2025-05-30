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

import kwasm.ast.type.MemoryType
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.type.readMemoryType
import kwasm.format.binary.type.toBytes
import kwasm.format.binary.value.readVector
import kwasm.format.binary.value.toBytesAsVector
import kwasm.util.Leb128

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#memory-section):
 *
 * The memory section has the id 5. It decodes into a vector of memories that represent the `mems`
 * component of a module.
 *
 * ```
 *      memsec  ::= mem*:section_5(vec(mem))    => mem*
 *      mem     ::= mt:memtype                  => {type mt}
 * ```
 */
fun BinaryParser.readMemorySection(): MemorySection = MemorySection(readVector { readMemoryType() })

/** Encodes the receiving [MemorySection] as a sequence of Bytes. */
fun MemorySection.toBytesNoHeader(): Sequence<Byte> {
    val vecBytes = memories.toBytesAsVector { it.toBytes() }.toList()
    return Leb128.encodeUnsigned(vecBytes.size) + vecBytes.asSequence()
}

/** Represents the Memory Section of a binary-encoded WebAssembly module. */
data class MemorySection(val memories: List<MemoryType>) : Section
