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

import kwasm.ast.type.Result
import kwasm.ast.type.ResultType
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readVector

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/types.html#result-types):
 *
 * Result types are encoded by the respective vectors of value types `.
 *
 * ```
 * resulttype   ::= t*:vec(valtype) =>  [t*]
 * ```
 */
fun BinaryParser.readResultType(): ResultType {
    val valueTypes = readVector { readValueType() }
    if (valueTypes.size > 1) throwException("At most one result allowed", - valueTypes.size)
    val valueType = valueTypes.firstOrNull() ?: return ResultType(null)
    return ResultType(Result(valueType))
}
