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

import kwasm.ast.instruction.ParametricInstruction
import kwasm.format.binary.BinaryParser

internal val PARAMETRIC_OPCODE_RANGE = 0x1A..0x1B

/**
 * From
 * [the docs](https://webassembly.github.io/spec/core/binary/instructions.html#parametric-instructions):
 *
 * Parametric instructions are represented by single byte codes.
 *
 * ```
 *      instr   ::= 0x1A => drop
 *                  0x1B => select
 * ```
 */
fun BinaryParser.readParametricInstruction(opcode: Int): ParametricInstruction = when (opcode) {
    0x1A -> ParametricInstruction.Drop
    0x1B -> ParametricInstruction.Select
    else -> throwException(
        "Invalid opcode for parametric instruction: ${opcode.toString(16)}",
        -1
    )
}
