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

import kwasm.ast.module.Offset
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.instruction.parseExpression
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.token.Token

/** Parses an [Offset] from the receiving [List] of [Token]s. */
fun List<Token>.parseOffset(
    fromIndex: Int,
    maxExpressionLength: Int = Int.MAX_VALUE
): ParseResult<Offset> {
    var currentIndex = fromIndex

    // Case when the 'offset' keyword is present.
    if (isOpenParen(currentIndex) && isKeyword(currentIndex + 1, "offset")) {
        currentIndex += 2
        val expression = parseExpression(currentIndex)
        parseCheck(
            contextAt(currentIndex),
            expression.astNode.instructions.size <= maxExpressionLength,
            "Expected at most $maxExpressionLength instructions for offset expression"
        )
        currentIndex += expression.parseLength
        parseCheck(contextAt(currentIndex), isClosedParen(currentIndex), "Expected ')'")
        currentIndex++
        return ParseResult(
            Offset(expression.astNode),
            currentIndex - fromIndex
        )
    }

    // Case when the 'offset' keyword is not present, we are allowed a single instruction.
    val expression = parseExpression(currentIndex)
    parseCheck(
        contextAt(currentIndex),
        expression.astNode.instructions.isNotEmpty(),
        "Expected offset expression"
    )
    currentIndex += expression.parseLength

    return ParseResult(
        Offset(expression.astNode),
        currentIndex - fromIndex
    )
}
