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

import kwasm.ast.AstNodeList
import kwasm.ast.astNodeListOf
import kwasm.ast.module.Local
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.token.Identifier
import kwasm.format.text.token.Token
import kwasm.format.text.type.parseValueTypes

/**
 * Parses a [Local] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#functions):
 *
 * ```
 *   local ::= ‘(’ ‘local’ id? t:valtype ‘)’ => t
 * ```
 */
fun List<Token>.parseLocal(fromIndex: Int): ParseResult<AstNodeList<Local>>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++
    if (!isKeyword(currentIndex, "local")) return null
    currentIndex++

    val id = (getOrNull(currentIndex) as? Identifier)?.let {
        currentIndex++
        kwasm.ast.Identifier.Local(it.value)
    }

    val valTypes = if (id == null) {
        parseValueTypes(currentIndex)
    } else {
        parseValueTypes(currentIndex, minRequired = 1, maxAllowed = 1)
    }
    currentIndex += valTypes.parseLength
    parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
    currentIndex++

    return ParseResult(
        if (valTypes.astNode.isEmpty() && id == null) {
            astNodeListOf()
        } else {
            AstNodeList(valTypes.astNode.map { Local(id, it) })
        },
        currentIndex - fromIndex
    )
}

/**
 * Parses an [AstNodeList] of [Local]s from the receiving [List] of [Token]s.
 *
 * See [parseLocal].
 */
fun List<Token>.parseLocals(fromIndex: Int): ParseResult<AstNodeList<Local>> {
    var currentIndex = fromIndex
    val result = mutableListOf<Local>()
    while (true) {
        val parsed = parseLocal(currentIndex)?.takeIf { it.astNode.isNotEmpty() } ?: break
        currentIndex += parsed.parseLength
        result.addAll(parsed.astNode)
    }
    return ParseResult(
        AstNodeList(result),
        currentIndex - fromIndex
    )
}
