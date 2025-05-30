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

import kwasm.ast.Identifier
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readVector

const val FUNCTION_TYPE_MAGIC: Byte = 0x60

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/types.html#function-types):
 *
 * Function types are encoded by the byte `0x60` followed by the respective vectors of parameter
 * and result types.
 *
 * ```
 *      functype    ::= 0x60 rt1:resulttype rt2:resulttype  =>  rt1 → rt2
 * ```
 */
fun BinaryParser.readFunctionType(): FunctionType {
    val byte = readByte()
    if (byte != FUNCTION_TYPE_MAGIC) {
        throwException("Expected 0x60 for Function Type (integer representation too long)", -1)
    }
    val params = readVector { readValueType() }.map { Param(Identifier.Local(null, null), it) }
    val results = readVector { readValueType() }.map { Result(it) }
    return FunctionType(params, results)
}
