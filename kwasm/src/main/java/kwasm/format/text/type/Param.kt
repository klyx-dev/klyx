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
import kwasm.ast.Identifier
import kwasm.ast.type.Param
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.parseIdentifier
import kwasm.format.text.token.Token

/**
 * Parses a Param from a list of Tokens.
 * from [the docs](https://webassembly.github.io/spec/core/text/types.html#function-types):
 *
 * ```
 *   param    ::=  ‘(’ ‘param’  id?  t:valtype ‘)’  => t
 * ```
 */
fun List<Token>.parseParam(fromIndex: Int): ParseResult<AstNodeList<Param>> {
    var currentIndex = fromIndex
    parseCheck(contextAt(currentIndex), isOpenParen(currentIndex), "Invalid Param: Expecting \"(\"")
    currentIndex++
    parseCheck(contextAt(currentIndex), isKeyword(currentIndex, "param"), "Invalid Param: Expecting \"param\"")
    currentIndex++
    val id = parseIdentifier<Identifier.Local>(currentIndex)
    currentIndex += id?.parseLength ?: 0
    val valueTypes = parseValueTypes(currentIndex, minRequired = 1)
    currentIndex += valueTypes.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Invalid Param: Expecting ) token")
    currentIndex++
    return ParseResult(
        AstNodeList(
            valueTypes.astNode.map {
                Param(
                    id?.astNode ?: Identifier.Local(null, null),
                    it
                )
            }
        ),
        currentIndex - fromIndex
    )
}
