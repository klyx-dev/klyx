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

import kwasm.ast.instruction.MemArg
import kwasm.format.parseCheck
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.token.IntegerLiteral
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * Parses a [MemArg] from the receiving [List] of [Token]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/instructions.html#text-memarg):
 *
 * ```
 *   memarg_N   ::= o:offset a:align_N  => { align n, offset o } (if a = 2^n)
 *   offset     ::= ‘offset=’ o:u32     => o
 *                  ϵ => 0
 *   align_N    ::= ‘align=’ a:u32      => a
 *                  ϵ => N
 * ```
 */
fun List<Token>.parseMemarg(fromIndex: Int, expectedMaxBytes: Int): ParseResult<MemArg> {
    var currentIndex = fromIndex
    val firstKeyword = getOrNull(currentIndex) as? Keyword
        ?: return ParseResult(MemArg(0, expectedMaxBytes).deDupe(), 0)

    val memArg = when {
        firstKeyword.value.startsWith("offset=") -> {
            val offset = IntegerLiteral.Unsigned(
                firstKeyword.value.substring(7),
                32,
                contextAt(currentIndex)
            ).value
            currentIndex++
            val alignment = (getOrNull(currentIndex) as? Keyword)
                ?.takeIf { it.value.startsWith("align=") }
                ?.let {
                    currentIndex++
                    IntegerLiteral.Unsigned(
                        it.value.substring(6),
                        32,
                        contextAt(currentIndex)
                    ).value
                }
            MemArg(
                offset.toInt(),
                alignment?.toInt() ?: expectedMaxBytes
            )
        }
        firstKeyword.value.startsWith("align=") -> {
            currentIndex++
            val alignment = IntegerLiteral.Unsigned(
                firstKeyword.value.substring(6),
                32,
                contextAt(currentIndex)
            ).value
            MemArg(0, alignment.toInt())
        }
        else -> MemArg(0, expectedMaxBytes)
    }

    parseCheck(contextAt(fromIndex), memArg.isAlignmentWellFormed()) {
        "Illegal MemArg value for N=$expectedMaxBytes (alignment)"
    }

    return ParseResult(
        memArg.deDupe(),
        currentIndex - fromIndex
    )
}
