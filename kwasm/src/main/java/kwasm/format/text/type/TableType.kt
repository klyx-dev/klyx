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

package kwasm.format.text.type

import kwasm.ast.type.ElementType
import kwasm.ast.type.TableType
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isKeyword
import kwasm.format.text.token.Token

/**
 * From [the spec](https://webassembly.github.io/spec/core/text/types.html#table-types):
 * ```
 *   tabletype ::= lim:limits  et:elemtype => lim et
 *   elemtype  ::= ‘funcref’               => funcref
 * ```
 */
fun List<Token>.parseTableType(startingIndex: Int): ParseResult<TableType> {
    var currentIndex = startingIndex
    val limits = parseLimits(startingIndex)
    currentIndex += limits.parseLength
    parseCheck(contextAt(currentIndex), isKeyword(currentIndex, "funcref"), "Expected 'funcref'")
    currentIndex++
    return ParseResult(
        TableType(limits.astNode, ElementType.FunctionReference),
        currentIndex - startingIndex
    )
}
