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

import kwasm.ast.type.FunctionType
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.type.readFunctionType
import kwasm.format.binary.value.readVector

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#type-section):
 *
 * The type section has the id 1. It decodes into a vector of function types that represent the 𝗍𝗒𝗉𝖾𝗌
 * component of a module.
 *
 * ```
 *      typesec ::= ft*:section_1(vec(functype)) => ft*
 * ```
 */
fun BinaryParser.readTypeSection(): TypeSection {
    try {
        return TypeSection(readVector { readFunctionType() })
    } catch (e: ParseException) {
        if ("Expected byte, but none found" in e.message ?: "") {
            throwException("Malformed: unexpected end of section or function")
        }
        throw e
    }
}

/**
 * Represents a type section in a binary-encoded WASM module.
 */
data class TypeSection(val functionTypes: List<FunctionType>) : Section
