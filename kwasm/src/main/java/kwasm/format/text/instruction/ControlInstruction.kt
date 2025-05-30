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

import kwasm.ast.Identifier
import kwasm.ast.astNodeListOf
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.instruction.ControlInstruction.Break
import kwasm.ast.instruction.ControlInstruction.BreakIf
import kwasm.ast.instruction.ControlInstruction.BreakTable
import kwasm.ast.instruction.ControlInstruction.Call
import kwasm.ast.instruction.ControlInstruction.CallIndirect
import kwasm.ast.instruction.ControlInstruction.NoOp
import kwasm.ast.instruction.ControlInstruction.Return
import kwasm.ast.instruction.ControlInstruction.Unreachable
import kwasm.ast.instruction.Instruction
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.asKeywordMatching
import kwasm.format.text.isKeyword
import kwasm.format.text.module.parseIndex
import kwasm.format.text.module.parseIndices
import kwasm.format.text.module.parseTypeUse
import kwasm.format.text.token.Token

/** This file contains parsing implementations for all of the various [ControlInstruction]s. */

/**
 * Attempts to parse a [ControlInstruction] from the receiving [Token] [List]. Returns `null` if
 * none was found.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/text/instructions.html#control-instructions):
 *
 * ```
 *   blockinstr(I)  ::=
 *      ‘block’ I′:label(I) rt:resulttype (in:instr(I′))* ‘end’ id?
 *          => block rt in* end (if id? = ϵ ∨ id? = label)
 *      ‘loop’ I′:label(I) rt:resulttype (in:instr(I′))* ‘end’ id?
 *          => loop rt in* end (if id? = ϵ ∨ id? = label)
 *      ‘if’ I′:label(I) rt:resulttype (in^1:instr(I′))∗ (‘else’ id?^1 (in^2:instr(I'))*)? ‘end’ id?^2
 *          => if rt in*^1 else in*2 end (if id?^1 = ϵ ∨ id?^1 = label, id?^2 = ϵ ∨ id?^2 = label)
 *   plaininstr(I)  ::= ‘unreachable’                                   => unreachable
 *                      ‘nop’                                           => nop
 *                      ‘br’ l:labelidx(I)                              => br l
 *                      ‘br_if’ l:labelidx(I)                           => br_if l
 *                      ‘br_table' l*:vec(labelidx(I)) l^N:labelidx(I)  => br_table l∗ l^N
 *                      ‘return’                                        => return
 *                      ‘call’ x:funcidx(I)                             => call x
 *                      ‘call_indirect’ x,I′:typeuse(I)                 => call_indirect x (if I′={})
 * ```
 */
fun List<Token>.parseControlInstruction(fromIndex: Int): ParseResult<out ControlInstruction>? =
    parseBlockOrLoopContInstruction(fromIndex)
        ?: parseIfContInstruction(fromIndex)
        ?: parseUnreachableContInstruction(fromIndex)
        ?: parseNoOpContInstruction(fromIndex)
        ?: parseReturnContInstruction(fromIndex)
        ?: parseBreakContInstruction(fromIndex)
        ?: parseBreakIfContInstruction(fromIndex)
        ?: parseBreakTableContInstruction(fromIndex)
        ?: parseCallContInstruction(fromIndex)
        ?: parseCallIndirectContInstruction(fromIndex)

private fun List<Token>.parseBlockOrLoopContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    var tokensParsed = 0

    val blockOpener = getOrNull(fromIndex)?.asKeywordMatching("block")
    val loopOpener = getOrNull(fromIndex)?.asKeywordMatching("loop")
    if (blockOpener == null && loopOpener == null) return null
    val opener = requireNotNull(blockOpener ?: loopOpener)
    tokensParsed++

    val label = parseLabel(fromIndex + tokensParsed)
    tokensParsed += label.parseLength
    val resultType = parseTypeUse(fromIndex + tokensParsed)
    tokensParsed += resultType.parseLength
    val instructions = parseInstructions(fromIndex + tokensParsed)
    tokensParsed += instructions.parseLength

    getOrNull(fromIndex + tokensParsed)?.asKeywordMatching("end")
        ?: throw ParseException(
            "Expected \"end\" for block ${opener.value}",
            getOrNull(fromIndex + tokensParsed)?.context
                ?: getOrNull(fromIndex + tokensParsed - 1)?.context
        )
    tokensParsed++

    val closingId = getOrNull(fromIndex + tokensParsed) as? kwasm.format.text.token.Identifier
    if (closingId?.value != null && closingId.value != label.astNode.stringRepr) {
        throw ParseException(
            "Identifier after \"end\": ${closingId.value} does not match ${opener.value} opening" +
                " identifier: ${label.astNode.stringRepr}",
            closingId.context ?: getOrNull(fromIndex + tokensParsed - 1)?.context
        )
    }
    if (closingId != null) tokensParsed++

    return if (opener.value == "block") {
        ParseResult(
            ControlInstruction.Block(
                if (label.parseLength > 0) label.astNode else null,
                resultType.astNode.toResultType(),
                instructions.astNode
            ),
            tokensParsed
        )
    } else {
        ParseResult(
            ControlInstruction.Loop(
                if (label.parseLength > 0) label.astNode else null,
                resultType.astNode.toResultType(),
                instructions.astNode
            ),
            tokensParsed
        )
    }
}

private fun List<Token>.parseIfContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    var tokensParsed = 0

    getOrNull(fromIndex)?.asKeywordMatching("if") ?: return null
    tokensParsed++

    val label = parseLabel(fromIndex + tokensParsed)
    tokensParsed += label.parseLength
    val resultType = parseTypeUse(fromIndex + tokensParsed)
    tokensParsed += resultType.parseLength

    val positiveInstructions = parseInstructions(fromIndex + tokensParsed)
    tokensParsed += positiveInstructions.parseLength

    // Parse the optional `else` block, and check its id
    val negativeInstructions =
        getOrNull(fromIndex + tokensParsed)
            ?.asKeywordMatching("else")
            ?.let {
                tokensParsed++

                val elseId =
                    getOrNull(fromIndex + tokensParsed) as? kwasm.format.text.token.Identifier

                if (elseId != null && elseId.value != label.astNode.stringRepr) {
                    throw ParseException(
                        "Identifier after \"else\": ${elseId.value} does not match if/else" +
                            " opening identifier: ${label.astNode.stringRepr}",
                        elseId.context
                    )
                }

                if (elseId != null) tokensParsed++
                parseInstructions(fromIndex + tokensParsed)
            }
            // There is no else block.
            ?: ParseResult(astNodeListOf<Instruction>(), 0)
    tokensParsed += negativeInstructions.parseLength

    getOrNull(fromIndex + tokensParsed)?.asKeywordMatching("end")
        ?: throw ParseException(
            "Expected \"end\" for if/else",
            getOrNull(fromIndex + tokensParsed)?.context
                ?: getOrNull(fromIndex + tokensParsed - 1)?.context
        )
    tokensParsed++

    val closingId = getOrNull(fromIndex + tokensParsed) as? kwasm.format.text.token.Identifier
    if (closingId != null && closingId.value != label.astNode.stringRepr) {
        throw ParseException(
            "Identifier after \"end\": ${closingId.value} does not match if/else opening" +
                " identifier: ${label.astNode.stringRepr}",
            closingId.context
        )
    }
    if (closingId != null) tokensParsed++

    return ParseResult(
        ControlInstruction.If(
            if (label.parseLength > 0) label.astNode else null,
            resultType.astNode.toResultType(),
            positiveInstructions.astNode,
            negativeInstructions.astNode
        ),
        tokensParsed
    )
}

private fun List<Token>.parseUnreachableContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? =
    getOrNull(fromIndex)?.asKeywordMatching("unreachable")?.let {
        ParseResult(
            Unreachable,
            1
        )
    }

private fun List<Token>.parseNoOpContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? =
    getOrNull(fromIndex)?.asKeywordMatching("nop")?.let {
        ParseResult(
            NoOp,
            1
        )
    }

private fun List<Token>.parseReturnContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? =
    getOrNull(fromIndex)?.asKeywordMatching("return")?.let {
        ParseResult(
            Return,
            1
        )
    }

private fun List<Token>.parseBreakContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    if (!isKeyword(fromIndex, "br")) return null
    val labelIndex = parseIndex<Identifier.Label>(fromIndex + 1)
    return ParseResult(Break(labelIndex.astNode), 2)
}

private fun List<Token>.parseBreakIfContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    getOrNull(fromIndex)?.asKeywordMatching("br_if") ?: return null

    val labelIndex = parseIndex<Identifier.Label>(fromIndex + 1)
    return ParseResult(BreakIf(labelIndex.astNode), 2)
}

private fun List<Token>.parseBreakTableContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    getOrNull(fromIndex)?.asKeywordMatching("br_table") ?: return null

    var nodesParsed = 1
    val parsedLabelIndexes = parseIndices<Identifier.Label>(fromIndex + nodesParsed, min = 1)
    nodesParsed += parsedLabelIndexes.parseLength

    val targets = parsedLabelIndexes.astNode.subList(0, parsedLabelIndexes.astNode.size - 1)
    val default = parsedLabelIndexes.astNode.last()

    return ParseResult(
        BreakTable(targets, default),
        nodesParsed
    )
}

private fun List<Token>.parseCallContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    getOrNull(fromIndex)?.asKeywordMatching("call") ?: return null

    val index = parseIndex<Identifier.Function>(fromIndex + 1)
    return ParseResult(Call(index.astNode), 2)
}

private fun List<Token>.parseCallIndirectContInstruction(
    fromIndex: Int
): ParseResult<out ControlInstruction>? {
    getOrNull(fromIndex)?.asKeywordMatching("call_indirect") ?: return null

    val typeUse = parseTypeUse(fromIndex + 1)
    return ParseResult(
        CallIndirect(typeUse.astNode),
        1 + typeUse.parseLength
    )
}
