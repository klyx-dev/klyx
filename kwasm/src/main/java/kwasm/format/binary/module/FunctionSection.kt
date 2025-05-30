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
import kwasm.format.binary.value.readVector
import kwasm.format.binary.value.toBytesAsVector
import kwasm.util.Leb128

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#function-section):
 *
 * The function section has the id 3. It decodes into a vector of type indices that represent the
 * `type` fields of the functions in the `funcs` component of a module. The `locals` and `body`
 * fields of the respective functions are encoded separately in the code section.
 *
 * ```
 *      funcsec ::= x*:section_3(vec(typeidx))  => x*
 * ```
 */
fun BinaryParser.readFunctionSection(): FunctionSection =
    FunctionSection(readVector { readIndex() })

/** Encodes the receiving [FunctionSection] as a sequence of Bytes. */
internal fun FunctionSection.toBytesNoHeader(): Sequence<Byte> {
    val vectorBytes = functionTypes.toBytesAsVector { it.toBytes() }.toList()
    return Leb128.encodeUnsigned(vectorBytes.size) + vectorBytes.asSequence()
}

/** Represents the function section of a binary-encoded WebAssembly module. */
data class FunctionSection(val functionTypes: List<Index<Identifier.Type>>) : Section
