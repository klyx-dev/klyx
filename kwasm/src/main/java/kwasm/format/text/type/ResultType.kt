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

import kwasm.ast.type.ResultType
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * Parses a Result from a list of Tokens.
 * From [the docs](https://webassembly.github.io/spec/core/text/types.html#result-types):
 *
 * ```
 *   resultType   ::=  (t:result)?  => [t?]
 * ```
 */
fun List<Token>.parseResultType(currentIndex: Int): ParseResult<ResultType> {
    val maybeResultKeyword = getOrNull(currentIndex + 1)
    if (maybeResultKeyword !is Keyword || maybeResultKeyword.value != "result") {
        return ParseResult(ResultType(null), 0)
    }
    val parsedResult = parseResult(currentIndex)
    parseCheck(
        contextAt(currentIndex),
        parsedResult.astNode.size == 1,
        "At most one result type allowed"
    )
    return ParseResult(
        ResultType(parsedResult.astNode.first()),
        parsedResult.parseLength
    )
}
