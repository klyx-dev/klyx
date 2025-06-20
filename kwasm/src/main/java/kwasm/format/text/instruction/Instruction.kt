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

package kwasm.format.text.instruction

import kwasm.ast.AstNodeList
import kwasm.ast.instruction.Instruction
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.token.Token

/**
 * Parses a [List] of [Instruction]s from the [Token]s.
 *
 * Throws [ParseException] if fewer than [min] are found, and returns at most [max].
 */
fun List<Token>.parseInstructions(
    fromIndex: Int,
    min: Int = 0,
    max: Int = Int.MAX_VALUE
): ParseResult<AstNodeList<out Instruction>> {
    if (fromIndex !in indices) {
        if (min == 0) return ParseResult(
            AstNodeList(emptyList()),
            0
        )
        throw ParseException(
            "Expected at least $min instruction${if (min > 1) "s" else ""}, found 0",
            getOrNull(fromIndex - 1)?.context
        )
    }

    val result = mutableListOf<Instruction>()
    var tokensParsed = 0
    var instructionsParsed = 0

    while (instructionsParsed < max && fromIndex + tokensParsed < size) {
        val foldedInstruction = parseFoldedInstruction(fromIndex + tokensParsed)
        var foldedWasNull = true
        if (foldedInstruction != null) {
            // If we found a folded instruction, unwrap its individual instructions into the result.
            result.addAll(foldedInstruction.astNode)
            tokensParsed += foldedInstruction.parseLength
            instructionsParsed += foldedInstruction.astNode.size
            foldedWasNull = false
        }

        val instruction = parseInstruction(fromIndex + tokensParsed)
            ?: if (foldedWasNull) break else continue
        result += instruction.astNode
        tokensParsed += instruction.parseLength
        instructionsParsed++
    }

    if (instructionsParsed < min) {
        throw ParseException(
            "Expected at least $min instruction${if (min > 1) "s" else ""}, " +
                "found $instructionsParsed",
            getOrNull(fromIndex)?.context ?: getOrNull(fromIndex - 1)?.context
        )
    }

    return ParseResult(AstNodeList(result), tokensParsed)
}

/**
 * Parses an [Instruction] from the receiving [Token]s [List].
 */
fun List<Token>.parseInstruction(fromIndex: Int): ParseResult<out Instruction>? {
    // TODO: Add to me by attempting to parse each type of instruction until all instruction types
    //       are covered.
    return parseControlInstruction(fromIndex)
        ?: parseMemoryInstruction(fromIndex)
        ?: parseNumericConstant(fromIndex)
        ?: parseNumericInstruction(fromIndex)
        ?: parseParametricInstruction(fromIndex)
        ?: parseVariableInstruction(fromIndex)
}
