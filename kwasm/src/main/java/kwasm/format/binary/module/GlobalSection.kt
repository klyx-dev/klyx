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
import kwasm.ast.module.Global
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.instruction.readExpression
import kwasm.format.binary.type.readGlobalType
import kwasm.format.binary.value.readVector

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#global-section):
 *
 * The global section has the id 6. It decodes into a vector of globals that represent the `globals`
 * component of a module.
 *
 * ```
 *      globalsec   ::= glob*:section_6(vec(global))    => glob*
 *      global      ::= gt:globaltype e:expr            => {type gt, init e}
 * ```
 */
fun BinaryParser.readGlobalSection(): GlobalSection = GlobalSection(readVector { readGlobal() })

/** Reads a global from the binary stream. */
internal fun BinaryParser.readGlobal(): Global =
    Global(Identifier.Global(null, null), readGlobalType(), readExpression())

/** Represents a global section from a binary-encoded WebAssembly module. */
data class GlobalSection(val globals: List<Global>) : Section
