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

import kwasm.ast.module.Type
import kwasm.ast.type.FunctionType
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.asKeywordMatching
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isOpenParen
import kwasm.format.text.token.Identifier
import kwasm.format.text.token.Token
import kwasm.format.text.type.parseFunctionType

/**
 * Parses a type as a [FunctionType] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#types):
 *
 * ```
 *   type ::= ‘(’ ‘type’ id? ft:functype ‘)’ => ft
 * ```
 */
fun List<Token>.parseType(
    fromIndex: Int,
    counts: TextModuleCounts,
): Pair<ParseResult<Type>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++

    getOrNull(currentIndex)?.asKeywordMatching("type") ?: return null
    currentIndex++

    val identifier = getOrNull(currentIndex) as? Identifier
    if (identifier != null) currentIndex++

    val funcType = parseFunctionType(currentIndex)
    currentIndex += funcType.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    val astTypeIdentifier = identifier?.let { kwasm.ast.Identifier.Type(identifier.value) }

    return ParseResult(
        Type(astTypeIdentifier, funcType.astNode),
        currentIndex - fromIndex
    ) to counts.copy(types = counts.types + 1)
}
