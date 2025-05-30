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

package kwasm.ast.type

import kwasm.ast.AstNode
import kwasm.ast.Identifier
import kwasm.ast.module.Index

/**
 * Data class to hold a ResultType's result
 * from [the docs](https://webassembly.github.io/spec/core/text/types.html#result-types):
 *
 * ```
 *   resultType   ::=  (t:result)?  => [t?]
 * ```
 */
data class ResultType(
    val result: Result?,
    val resultIndex: Index<Identifier.Type>? = null
) : AstNode {
    override fun toString(): String = if (resultIndex == null) {
        "(result${if (result != null) " $result" else ""})"
    } else "(result typeidx: $resultIndex)"
}
