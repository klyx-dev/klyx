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

import kwasm.ast.Identifier
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.module.Index
import kwasm.ast.module.TypeUse
import kwasm.ast.type.Result
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.module.readIndex
import kwasm.format.binary.value.readLong
import kwasm.format.binary.value.readVector

internal val CONTROL_OPCODE_RANGE = 0x00..0x11

/**
 * From
 * [the docs](https://webassembly.github.io/spec/core/binary/instructions.html#control-instructions):
 *
 * Control instructions have varying encodings. For structured instructions, the instruction
 * sequences forming nested blocks are terminated with explicit opcodes for `end` and `else`.
 *
 * Block types are encoded in special compressed form, by either the byte `0x40` indicating the
 * empty type, as a single value type, or as a type index encoded as a positive signed integer.
 *
 * ```
 *      blocktype   ::= 0x40                                => ϵ
 *                      t:valtype                           => t
 *                      x:s33                               => x (if x >= 0)
 *      instr       ::= 0x00                                => unreachable
 *                      0x01                                => nop
 *                      0x02 bt:blocktype (in:instr)* 0x0B  => block bt in* end
 *                      0x03 bt:blocktype (in:instr)* 0x0B  => loop bt in* end
 *                      0x04 bt:blocktype (in:instr)* 0x0B  => if bt in* else ϵ end
 *                      0x04 bt:blocktype (in1:instr)* 0x05 (in2:instr)* 0x0B
 *                                                          => if bt in*1 else in*2 end
 *                      0x0C l:labelidx                     => br l
 *                      0x0D l:labelidx                     => br_if l
 *                      0x0E l*:vec(labelidx) l_N:labelidx  => br_table l* l_N
 *                      0x0F                                => return
 *                      0x10 x:funcidx                      => call x
 *                      0x11 x:typeidx 0x00                 => call_indirect x
 * ```
 *
 * **Note**
 * The `else` opcode `0x05` in the encoding of an `if` instruction can be omitted if the following
 * instruction sequence is empty.
 *
 * Unlike any other occurrence, the type index in a block type is encoded as a positive signed
 * integer, so that its signed LEB128 bit pattern cannot collide with the encoding of value types or
 * the special code `0x40`, which correspond to the LEB128 encoding of negative integers. To avoid
 * any loss in the range of allowed indices, it is treated as a 33 bit signed integer.
 *
 * In future versions of WebAssembly, the zero byte occurring in the encoding of the `call_indirect`
 * instruction may be used to index additional tables.
 */
fun BinaryParser.readControlInstruction(opcode: Int): ControlInstruction = when (opcode) {
    0x00 -> ControlInstruction.Unreachable
    0x01 -> ControlInstruction.NoOp
    in 0x02..0x04 -> readBlock(opcode)
    0x0C -> ControlInstruction.Break(readIndex())
    0x0D -> ControlInstruction.BreakIf(readIndex())
    0x0E -> ControlInstruction.BreakTable(readVector { readIndex() }, readIndex())
    0x0F -> ControlInstruction.Return
    0x10 -> ControlInstruction.Call(readIndex())
    0x11 ->
        ControlInstruction.CallIndirect(TypeUse(readIndex(), emptyList(), emptyList()))
            .also {
                if (readByte() != 0x00.toByte()) {
                    throwException("Invalid table index for call_indirect (zero flag expected)", -1)
                }
            }
    else -> throwException("Invalid opcode for instruction: 0x${opcode.toString(16)}", -1)
}

@Suppress("UNCHECKED_CAST")
internal fun BinaryParser.readBlock(opcode: Int): ControlInstruction {
    val blockType = readLong()
    val blockValueType = when (blockType) {
        -1L -> ValueType.I32
        -2L -> ValueType.I64
        -3L -> ValueType.F32
        -4L -> ValueType.F64
        else -> null
    }
    val resultType = when {
        blockType == -64L -> ResultType(null)
        blockValueType != null -> ResultType(Result(blockValueType))
        else -> {
            ResultType(
                null,
                Index.ByInt(blockType.toInt()) as Index<Identifier.Type>
            )
        }
    }
    return when (opcode) {
        0x02 -> ControlInstruction.Block(null, resultType, readExpression().instructions)
        0x03 -> ControlInstruction.Loop(null, resultType, readExpression().instructions)
        0x04 -> readIf(resultType)
        else -> throwException("Invalid control instruction format")
    }
}

internal fun BinaryParser.readIf(resultType: ResultType): ControlInstruction.If {
    val positiveBranch = readExpression().instructions
    val negativeBranch =
        if (lastByte == 0x05.toByte()) readExpression().instructions else emptyList()
    return ControlInstruction.If(null, resultType, positiveBranch, negativeBranch)
}
