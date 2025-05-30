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
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.instruction.Instruction
import kwasm.format.ParseException
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.getOrThrow
import kwasm.format.text.isOpenParen
import kwasm.format.text.module.parseTypeUse
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token

/**
 * Parses a possibly-empty [AstNodeList] of folded [Instruction]s into an unwrapped list of
 * non-folded instructions.
 *
 * See [parseFoldedInstruction].
 */
fun List<Token>.parseFoldedInstructions(fromIndex: Int): ParseResult<AstNodeList<out Instruction>> {
    var currentIndex = fromIndex
    val result = mutableListOf<Instruction>()

    while (true) {
        val folded = parseFoldedInstruction(currentIndex) ?: break
        currentIndex += folded.parseLength
        result.addAll(folded.astNode)
    }

    return ParseResult(
        AstNodeList(result),
        currentIndex - fromIndex
    )
}

/**
 * From
 * [the docs](https://webassembly.github.io/spec/core/text/instructions.html#folded-instructions):
 *
 * Instructions can be written as S-expressions by grouping them into folded form. In that notation,
 * an instruction is wrapped in parentheses and optionally includes nested folded instructions to
 * indicate its operands.
 *
 * In the case of [kwasm.ast.ControlInstruction.Block]/[kwasm.ast.ControlInstruction.Loop]
 * instructions, the folded form omits the `end` delimiter. For [kwasm.ast.ControlInstruction.If]
 * instructions, both branches have to be wrapped into nested S-expressions, headed by the keywords
 * `then` and `else`.
 */
fun List<Token>.parseFoldedInstruction(fromIndex: Int): ParseResult<AstNodeList<out Instruction>>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++

    val keyword: Keyword =
        getOrThrow(currentIndex, "Expected `if`, `block`, `loop`, or a \"plain\" instruction")

    val instructions: List<Instruction> = when {
        keyword.value == "block" || keyword.value == "loop" -> {
            currentIndex++
            val foldedBlock = parseFoldedBlockOrLoop(currentIndex, keyword.value == "loop")
            currentIndex += foldedBlock.parseLength
            listOf(foldedBlock.astNode)
        }
        keyword.value == "if" -> {
            currentIndex++
            val foldedIf = parseFoldedIf(currentIndex)
            currentIndex += foldedIf.parseLength
            foldedIf.astNode
        }
        // Don't try to parse folded-if's then/else here.
        keyword.value == "then" || keyword.value == "else" -> return null
        else -> {
            val plainInstruction = parseInstruction(currentIndex)?.takeIf { it.astNode.isPlain }
                ?: throw ParseException(
                    "Expected `if`, `block`, `loop`, or a \"plain\" instruction (unknown operator)",
                    contextAt(currentIndex)
                )
            /*
             * ‘(’ plaininstr foldedinstr* ‘)’ ≡ foldedinstr* plaininstr
             */
            currentIndex += plainInstruction.parseLength
            val foldedInstructions = parseFoldedInstructions(currentIndex)
            currentIndex += foldedInstructions.parseLength
            val foldedList = foldedInstructions.astNode as List<Instruction>
            foldedList + plainInstruction.astNode
        }
    }

    getOrThrow<Paren.Closed>(
        currentIndex,
        "Expected ')' after folded instruction: ${keyword.value}"
    )
    currentIndex++
    return ParseResult(
        AstNodeList(instructions),
        currentIndex - fromIndex
    )
}

/**
 * Parses a folded [ControlInstruction.Block] or [ControlInstruction.Loop] from the receiver.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/text/instructions.html#folded-instructions):
 *
 * ```
 *   ‘(’ ‘block’ label resulttype instr* ‘)’ ≡ ‘block’ label resulttype instr* ‘end’
 *   ‘(‘ ’loop’  label resulttype instr* ‘)’ ≡ ‘loop’  label resulttype instr* ‘end’
 * ```
 */
private fun List<Token>.parseFoldedBlockOrLoop(
    fromIndex: Int,
    isLoop: Boolean = false
): ParseResult<out Instruction> {
    // Note: The opening paren and `block`/`loop` have already been parsed.
    var currentIndex = fromIndex
    val label = parseLabel(currentIndex)
    currentIndex += label.parseLength
    val resultType = parseTypeUse(currentIndex)
    currentIndex += resultType.parseLength
    val instructions = parseInstructions(currentIndex)
    currentIndex += instructions.parseLength
    // Note: The closing paren will be parsed by the caller.
    return if (isLoop) {
        ParseResult(
            ControlInstruction.Loop(
                if (label.parseLength > 0) label.astNode else null,
                resultType.astNode.toResultType(),
                instructions.astNode
            ),
            currentIndex - fromIndex
        )
    } else {
        ParseResult(
            ControlInstruction.Block(
                if (label.parseLength > 0) label.astNode else null,
                resultType.astNode.toResultType(),
                instructions.astNode
            ),
            currentIndex - fromIndex
        )
    }
}

/**
 * From
 * [the docs](https://webassembly.github.io/spec/core/text/instructions.html#folded-instructions):
 *
 * ```
 *   ‘(’ ‘if’ label resulttype foldedinstr* ‘(’ ‘then’ instr*1 ‘)’ ‘(’ ‘else’ instr*2 ‘)’? ‘)’
 *                                         ≡
 *   foldedinstr* ‘if’ label resulttypeinstr*1 ‘else’ (instr*2)? ‘end’
 * ```
 */
private fun List<Token>.parseFoldedIf(fromIndex: Int): ParseResult<AstNodeList<out Instruction>> {
    // The '(' and 'if' have been parsed by the caller.
    var currentIndex = fromIndex

    val label = parseLabel(currentIndex)
    currentIndex += label.parseLength
    val resultType = parseTypeUse(currentIndex)
    currentIndex += resultType.parseLength
    val foldedInstructions = parseFoldedInstructions(currentIndex)
    currentIndex += foldedInstructions.parseLength

    getOrThrow<Paren.Open>(currentIndex, "Expected 'then'")
    currentIndex++
    val keyword = getOrThrow<Keyword>(currentIndex, "Expected 'then'")
    parseCheck(contextAt(currentIndex), keyword.value == "then", "Expected 'then'")
    currentIndex++
    val thenInstructions = parseInstructions(currentIndex)
    currentIndex += thenInstructions.parseLength
    getOrThrow<Paren.Closed>(currentIndex, "Expected ')'")
    currentIndex++

    val elseInstructions = mutableListOf<Instruction>()
    if (isOpenParen(currentIndex)) {
        currentIndex++
        // Check for "else"
        val elseKeyword = getOrThrow<Keyword>(currentIndex, "Expecting 'else'")
        parseCheck(contextAt(currentIndex), elseKeyword.value == "else", "Expecting 'else'")
        currentIndex++

        val elseInstructionsNode = parseInstructions(currentIndex)
        currentIndex += elseInstructionsNode.parseLength
        elseInstructions.addAll(elseInstructionsNode.astNode)

        getOrThrow<Paren.Closed>(currentIndex, "Expected ')'")
        currentIndex++
    }

    // The ')' will be parsed by the caller.
    return ParseResult(
        AstNodeList(
            foldedInstructions.astNode + ControlInstruction.If(
                label.astNode,
                resultType.astNode.toResultType(),
                thenInstructions.astNode,
                elseInstructions
            )
        ),
        currentIndex - fromIndex
    )
}
