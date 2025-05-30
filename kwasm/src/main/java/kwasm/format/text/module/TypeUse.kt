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

package kwasm.format.text.module

import kwasm.ast.Identifier
import kwasm.ast.module.TypeUse
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.isKeyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token
import kwasm.format.text.type.parseParamList
import kwasm.format.text.type.parseResultList

/**
 * Parses a [TypeUse] out of the [Token] [List] according to the following grammar from
 * [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-typeuse).
 *
 * For the required runtime checks are see [TypeUse]:
 *
 * ```
 *   typeuse(I) ::=
 *          empty                                                   => TypeUse(null)
 *          (t_1:param)* (t_2:result)*                              => TypeUse(x, [t_1], [t_2])
 *          ‘(’ ‘type’ x:typeidx(I) ‘)’                             => TypeUse(x)
 *          ‘(’ ‘type’ x:typeidx(I) ‘)’ (t_1:param)* (t_2:result)*  => TypeUse(x, [t_1], [t_2])
 * ```
 */
fun List<Token>.parseTypeUse(fromIndex: Int): ParseResult<TypeUse> {
    var tokensParsed = 0

    val typeIndex = if (
        getOrNull(fromIndex) is Paren.Open &&
        getOrNull(fromIndex + 1)?.isKeyword("type") == true
    ) {
        tokensParsed += 2
        val typeIndex = parseIndex<Identifier.Type>(fromIndex + 2)
        tokensParsed += typeIndex.parseLength
        if (getOrNull(fromIndex + tokensParsed) !is Paren.Closed) {
            throw ParseException(
                "Expected \")\"",
                getOrNull(fromIndex + tokensParsed)?.context
                    ?: getOrNull(fromIndex + tokensParsed - 1)?.context
            )
        }
        tokensParsed++
        typeIndex.astNode
    } else null

    val params = parseParamList(fromIndex + tokensParsed)
    tokensParsed += params.parseLength
    val results = parseResultList(fromIndex + tokensParsed)
    tokensParsed += results.parseLength

    return ParseResult(
        TypeUse(
            typeIndex,
            params.astNode,
            results.astNode
        ),
        tokensParsed
    )
}
