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
import kwasm.ast.module.StartFunction
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.token.Token

/**
 * Parses a [StartFunction] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#start-function):
 *
 * A start function is defined in terms of its index.
 *
 * ```
 *   start_I ::= ‘(’ ‘start’ x:funcidx_I ‘)’ => {func x}
 * ```
 */
fun List<Token>.parseStartFunction(
    fromIndex: Int,
    counts: TextModuleCounts,
): Pair<ParseResult<StartFunction>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(fromIndex)) return null
    currentIndex++

    if (getOrNull(currentIndex)?.isKeyword("start") != true) return null
    currentIndex++

    val funcIndex = parseIndex<Identifier.Function>(currentIndex)
    currentIndex += funcIndex.parseLength

    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        StartFunction(funcIndex.astNode),
        currentIndex - fromIndex
    ) to counts
}
