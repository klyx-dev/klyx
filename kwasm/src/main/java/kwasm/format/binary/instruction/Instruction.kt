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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package kwasm.format.binary.instruction

import kwasm.ast.instruction.Instruction
import kwasm.format.binary.BinaryParser

/**
 * Parses an instruction given the opcode byte provided, or throws a [ParseException] if no
 * instruction can be found for the provided opcode. Returns null if `end` was read.
 *
 * From [the docs](https://webassembly.github.io/spec/core/binary/instructions.html#instructions):
 *
 * Instructions are encoded by opcodes. Each opcode is represented by a single byte, and is followed
 * by the instruction’s immediate arguments, where present. The only exception are structured
 * control instructions, which consist of several opcodes bracketing their nested instruction
 * sequences.
 *
 * **Note**
 *
 * Gaps in the byte code ranges for encoding instructions are reserved for future extensions.
 */
fun BinaryParser.readInstruction(): Instruction? = when (val opcode = readByte().toUByte().toInt()) {
    END_BYTE,
    ELSE_BRANCH_BYTE -> null
    in CONTROL_OPCODE_RANGE -> readControlInstruction(opcode)
    in MEMORY_OPCODE_RANGE -> readMemoryInstruction(opcode)
    in NUMERIC_OPCODE_RANGE -> readNumericInstruction(opcode)
    NUMERIC_SATURATING_TRUNCATION_OPCODE -> readNumericInstruction(opcode)
    in PARAMETRIC_OPCODE_RANGE -> readParametricInstruction(opcode)
    in VARIABLE_OPCODE_RANGE -> readVariableInstruction(opcode)
    else -> throwException("No instruction defined for opcode: 0x${opcode.toString(16)}", -1)
}

private const val END_BYTE = 0x0B
internal const val ELSE_BRANCH_BYTE = 0x05
