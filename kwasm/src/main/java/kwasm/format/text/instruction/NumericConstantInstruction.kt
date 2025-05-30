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

import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.format.text.ParseResult
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * Attempts to parse a [NumericConstantInstruction] from the receiving [List] of [Token]s.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/text/instructions.html#numeric-instructions):
 *
 * ```
 *   plaininstrI    ::= ‘i32.const’ n:i32 => i32.const n
 *                      ‘i64.const’ n:i64 => i64.const n
 *                      ‘f32.const’ z:f32 => f32.const z
 *                      ‘f64.const’ z:f64 => f64.const z
 * ```
 */
fun List<Token>.parseNumericConstant(
    fromIndex: Int
): ParseResult<out NumericConstantInstruction<*>>? {
    var currentIndex = fromIndex
    val keyword = getOrNull(currentIndex) as? Keyword ?: return null
    currentIndex++

    val instruction = when (keyword.value) {
        "i32.const" ->
            parseLiteral(currentIndex, Int::class).let {
                currentIndex += it.parseLength
                NumericConstantInstruction.I32(it.astNode)
            }
        "i64.const" ->
            parseLiteral(currentIndex, Long::class).let {
                currentIndex += it.parseLength
                NumericConstantInstruction.I64(it.astNode)
            }
        "f32.const" ->
            parseLiteral(currentIndex, Float::class).let {
                currentIndex += it.parseLength
                NumericConstantInstruction.F32(it.astNode)
            }
        "f64.const" ->
            parseLiteral(currentIndex, Double::class).let {
                currentIndex += it.parseLength
                NumericConstantInstruction.F64(it.astNode)
            }
        else -> null
    } ?: return null

    return ParseResult(instruction, currentIndex - fromIndex)
}
