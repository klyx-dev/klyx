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
import kwasm.ast.instruction.VariableInstruction
import kwasm.format.text.ParseResult
import kwasm.format.text.module.parseIndex
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * Attempts to parse a [VariableInstruction] from the receiving [List] of [Token]s.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/text/instructions.html#variable-instructions):
 *
 * ```
 *   plaininstrI    ::= ‘local.get’ x:localidx => local.get x
 *                      ‘local.set’ x:localidx => local.set x
 *                      ‘local.tee’ x:localidx => local.tee x
 *                      ‘global.get’ x:globalidx => global.get x
 *                      ‘global.set’ x:globalidx => global.set x
 * ```
 */
fun List<Token>.parseVariableInstruction(fromIndex: Int): ParseResult<out VariableInstruction>? {
    var currentIndex = fromIndex
    val keyword = getOrNull(currentIndex) as? Keyword ?: return null
    currentIndex++

    val instruction = when (keyword.value) {
        "local.get" ->
            parseIndex<Identifier.Local>(currentIndex).let {
                currentIndex += it.parseLength
                VariableInstruction.LocalGet(it.astNode)
            }
        "local.set" ->
            parseIndex<Identifier.Local>(currentIndex).let {
                currentIndex += it.parseLength
                VariableInstruction.LocalSet(it.astNode)
            }
        "local.tee" ->
            parseIndex<Identifier.Local>(currentIndex).let {
                currentIndex += it.parseLength
                VariableInstruction.LocalTee(it.astNode)
            }
        "global.get" ->
            parseIndex<Identifier.Global>(currentIndex).let {
                currentIndex += it.parseLength
                VariableInstruction.GlobalGet(it.astNode)
            }
        "global.set" ->
            parseIndex<Identifier.Global>(currentIndex).let {
                currentIndex += it.parseLength
                VariableInstruction.GlobalSet(it.astNode)
            }
        else -> null
    } ?: return null

    return ParseResult(
        instruction as VariableInstruction,
        currentIndex - fromIndex
    )
}
