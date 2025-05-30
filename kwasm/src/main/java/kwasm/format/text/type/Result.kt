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

import kwasm.ast.AstNodeList
import kwasm.ast.type.Result
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.token.Token

/**
 * Parses a Result from a list of Tokens.
 * from [the docs](https://webassembly.github.io/spec/core/text/types.html#function-types):
 *
 * ```
 *   result   ::=  ‘(’ ‘result’  t:valtype ‘)’  => t
 * ```
 */
fun List<Token>.parseResult(fromIndex: Int): ParseResult<AstNodeList<Result>> {
    var currentIndex = fromIndex
    parseCheck(contextAt(currentIndex), isOpenParen(currentIndex), "Invalid Result: Expecting \"(\"")
    currentIndex++
    parseCheck(contextAt(currentIndex), isKeyword(currentIndex, "result"), "Invalid Result: Expecting \"result\"")
    currentIndex++
    val valueTypes = parseValueTypes(currentIndex, minRequired = 1)
    currentIndex += valueTypes.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Invalid Result: Expecting \")\"")
    currentIndex++
    return ParseResult(
        AstNodeList(valueTypes.astNode.map { Result(it) }),
        currentIndex - fromIndex
    )
}
