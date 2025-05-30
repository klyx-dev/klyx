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

import kwasm.ast.instruction.VariableInstruction
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.module.readIndex

internal val VARIABLE_OPCODE_RANGE = 0x20..0x24

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/instructions.html#variable-instructions):
 *
 * Variable instructions are represented by byte codes followed by the encoding of the respective index.
 *
 * ```
 *      instr ::=   0x20 x:localidx     => local.get x
 *                  0x21 x:localidx     => local.set x
 *                  0x22 x:localidx     => local.tee x
 *                  0x23 x:globalidx    => global.get x
 *                  0x24 x:globalidx    => global.set x
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun BinaryParser.readVariableInstruction(opcode: Int): VariableInstruction = when (opcode) {
    0x20 -> VariableInstruction.LocalGet(readIndex())
    0x21 -> VariableInstruction.LocalSet(readIndex())
    0x22 -> VariableInstruction.LocalTee(readIndex())
    0x23 -> VariableInstruction.GlobalGet(readIndex())
    0x24 -> VariableInstruction.GlobalSet(readIndex())
    else -> throwException("Bad opcode for VariableInstruction: 0x${opcode.toString(16)}", -1)
}
