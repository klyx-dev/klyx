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
import kwasm.ast.module.ElementSegment
import kwasm.ast.module.Index
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.token.Token

/**
 * Parses an [ElementSegment] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#element-segments):
 *
 * ```
 *   elem_I ::= ‘(’ ‘elem’ x:tableidx_I ‘(’ ‘offset’ e:expr_I ‘)’ y*:vec(funcidx_I) ‘)’
 *   \          => {table x, offset e, init y*}
 * ```
 */
@Suppress("UNCHECKED_CAST", "EXPERIMENTAL_UNSIGNED_LITERALS")
fun List<Token>.parseElementSegment(
    fromIndex: Int,
    counts: TextModuleCounts,
): Pair<ParseResult<ElementSegment>, TextModuleCounts>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "elem")) return null
    currentIndex++

    val tableIndex = if (!isOpenParen(currentIndex)) {
        parseIndex<Identifier.Table>(currentIndex)
    } else {
        // Empty is okay, we just need to set it to zero.
        ParseResult(
            Index.ByInt(0) as Index<Identifier.Table>,
            0
        )
    }
    currentIndex += tableIndex.parseLength

    val offset = parseOffset(currentIndex)
    currentIndex += offset.parseLength

    val funcIndices = parseIndices<Identifier.Function>(currentIndex)
    currentIndex += funcIndices.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        ElementSegment(
            tableIndex.astNode,
            offset.astNode,
            funcIndices.astNode
        ),
        currentIndex - fromIndex
    ) to counts
}
