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

import kwasm.ast.type.GlobalType
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token

/**
 * Parses a GlobalType from a list of Tokens.
 * From [the docs](https://webassembly.github.io/spec/core/text/types.html#global-types):
 *
 * ```
 *   globaltype ::=  t:valtype                => const t
 *                   ‘(’ ‘mut’  t:valtype ‘)’ => var t
 * ```
 */
fun List<Token>.parseGlobalType(currentIndex: Int): ParseResult<GlobalType> {
    val maybeOpenParen = this[currentIndex]
    return if (maybeOpenParen is Paren.Open) {
        val keyword = this[currentIndex + 1]
        if (keyword !is Keyword || keyword.value != "mut") {
            throw ParseException("Invalid GlobalType: Expecting \"mut\"", keyword.context)
        }
        val valueTypeParseResult = this.parseValueType(currentIndex + 2)
        val closeParen = this[currentIndex + valueTypeParseResult.parseLength + 2]
        if (closeParen !is Paren.Closed) {
            throw ParseException("Invalid GlobalType: Expecting \")\"", closeParen.context)
        }
        ParseResult(
            GlobalType(
                valueTypeParseResult.astNode,
                true
            ),
            valueTypeParseResult.parseLength + 3
        )
    } else {
        val valueTypeParseResult = this.parseValueType(currentIndex)
        ParseResult(
            GlobalType(
                valueTypeParseResult.astNode,
                false
            ),
            valueTypeParseResult.parseLength
        )
    }
}
