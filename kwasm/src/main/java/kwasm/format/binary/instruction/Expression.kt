/*
 * Copyright 2021 Google LLC
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

package kwasm.format.binary.instruction

import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.Instruction
import kwasm.format.binary.BinaryParser

/**
 * From [the docs]():
 *
 * Expressions are encoded by their instruction sequence terminated with an explicit `0x0B`
 * opcode for end.
 *
 * ```
 *      expr ::= (in:instr)* 0x0B => in* end
 * ```
 */
fun BinaryParser.readExpression(): Expression {
    val instructions = mutableListOf<Instruction>()
    do {
        // Read instruction returns `null` when the end opcode is seen.
        val instruction = readInstruction()
            ?.also { instructions.add(it) }
    } while (instruction != null)
    return Expression(instructions)
}
