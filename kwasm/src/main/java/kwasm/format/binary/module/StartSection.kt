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

import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.format.binary.BinaryParser
import kwasm.util.Leb128

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#start-section):
 *
 * The start section has the id 8. It decodes into an optional start function that represents the
 * `start` component of a module.
 *
 * ```
 *      startsec    ::= st?:section_8(start)    => st?
 *      start       ::= x:funcidx               => {func x}
 * ```
 */
fun BinaryParser.readStartSection(): StartSection = StartSection(readIndex())

/** Encodes a [StartSection] as a sequence of Bytes. */
internal fun StartSection.toBytesNoHeader(): Sequence<Byte> {
    val indexBytes = funcIndex.toBytes().toList()
    return Leb128.encodeUnsigned(indexBytes.size) + indexBytes.asSequence()
}

/** Represents a start section from a binary-encoded WebAssembly module. */
data class StartSection(val funcIndex: Index<Identifier.Function>) : Section
