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
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token

/**
 * Parses a FunctionType from a list of Tokens.
 * From [the docs](https://webassembly.github.io/spec/core/text/types.html#function-types):
 *
 * ```
 *   functype ::=  ‘(’ ‘func’  t*1:vec(param)  t*2:vec(result) ‘)’ => [t*1]→[t*2]
 *   param    ::=  ‘(’ ‘param’  id?  t:valtype ‘)’                 => t
 *   result   ::=  ‘(’ ‘result’  t:valtype ‘)’                     => t
 * ```
 */
fun List<Token>.parseFunctionType(fromIndex: Int): ParseResult<FunctionType> {
    var currentIndex = fromIndex
    parseCheck(contextAt(currentIndex), isOpenParen(currentIndex), "Expected '('")
    currentIndex++
    parseCheck(
        contextAt(currentIndex),
        getOrNull(currentIndex) !is Keyword || isKeyword(currentIndex, "func"),
        "Invalid FunctionType: Expecting \"func\""
    )
    currentIndex++
    val parsedParamList = parseParamList(currentIndex)
    currentIndex += parsedParamList.parseLength

    val parsedResultList = parseResultList(currentIndex)
    currentIndex += parsedResultList.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++
    return ParseResult(
        FunctionType(
            parsedParamList.astNode,
            parsedResultList.astNode
        ),
        currentIndex - fromIndex
    )
}

/**
 * Parses a list of Param from a list of tokens.
 * @return a Pair containing the list of Params and an integer tracking the number of tokens parsed
 */
fun List<Token>.parseParamList(currentIndex: Int): ParseResult<AstNodeList<Param>> {
    var parsedTokens = 0
    var nextParamIndex = currentIndex
    val paramList = mutableListOf<Param>()
    while (this.size > nextParamIndex && this[nextParamIndex] is Paren.Open) {
        val paramKeyword = this[nextParamIndex + 1]
        if (paramKeyword is Keyword && paramKeyword.value == "param") {
            val parsedParamResult = this.parseParam(nextParamIndex)
            paramList.addAll(parsedParamResult.astNode)
            nextParamIndex += parsedParamResult.parseLength
            parsedTokens += parsedParamResult.parseLength
        } else break
    }
    return ParseResult(AstNodeList(paramList), parsedTokens)
}

/**
 * Parses a list of Result from a list of tokens.
 * @return a Pair containing the list of Results and an integer tracking the number of tokens parsed
 */
fun List<Token>.parseResultList(currentIndex: Int): ParseResult<AstNodeList<Result>> {
    var parsedTokens = 0
    var nextResultIndex = currentIndex
    val resultList = mutableListOf<Result>()
    while (this.size > nextResultIndex && this[nextResultIndex] is Paren.Open) {
        val resultKeyword = this[nextResultIndex + 1]
        if (resultKeyword is Keyword && resultKeyword.value == "result") {
            val parsedResultResult = parseResult(nextResultIndex)
            resultList.addAll(parsedResultResult.astNode)
            nextResultIndex += parsedResultResult.parseLength
            parsedTokens += parsedResultResult.parseLength
        } else break
    }
    return ParseResult(AstNodeList(resultList), parsedTokens)
}
