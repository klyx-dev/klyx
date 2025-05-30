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

import kwasm.ast.instruction.Instruction
import kwasm.ast.instruction.MemArg
import kwasm.ast.instruction.MemoryInstruction
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readUInt

internal val MEMORY_OPCODE_RANGE = 0x28..0x40

/**
 * From
 * [the docs](https://webassembly.github.io/spec/core/binary/instructions.html#memory-instructions):
 *
 * Each variant of memory instruction is encoded with a different byte code. Loads and stores are
 * followed by the encoding of their `memarg` immediate.
 *
 * ```
 *      memarg  ::=     a:u32 o:u32 => {align a, offset o}
 *      instr   ::=     0x28 m:memarg => i32.load m
 *                      0x29 m:memarg => i64.load m
 *                      0x2A m:memarg => f32.load m
 *                      0x2B m:memarg => f64.load m
 *                      0x2C m:memarg => i32.load8_s m
 *                      0x2D m:memarg => i32.load8_u m
 *                      0x2E m:memarg => i32.load16_s m
 *                      0x2F m:memarg => i32.load16_u m
 *                      0x30 m:memarg => i64.load8_s m
 *                      0x31 m:memarg => i64.load8_u m
 *                      0x32 m:memarg => i64.load16_s m
 *                      0x33 m:memarg => i64.load16_u m
 *                      0x34 m:memarg => i64.load32_s m
 *                      0x35 m:memarg => i64.load32_u m
 *                      0x36 m:memarg => i32.store m
 *                      0x37 m:memarg => i64.store m
 *                      0x38 m:memarg => f32.store m
 *                      0x39 m:memarg => f64.store m
 *                      0x3A m:memarg => i32.store8 m
 *                      0x3B m:memarg => i32.store16 m
 *                      0x3C m:memarg => i64.store8 m
 *                      0x3D m:memarg => i64.store16 m
 *                      0x3E m:memarg => i64.store32 m
 *                      0x3F 0x00     => memory.size
 *                      0x40 0x00     => memory.grow
 * ```
 *
 * **Note**
 * In future versions of WebAssembly, the additional zero bytes occurring in the encoding of the
 * `memory.size` and `memory.grow` instructions may be used to index additional memories.
 */
fun BinaryParser.readMemoryInstruction(opcode: Int): Instruction = when (opcode) {
    0x3F -> {
        if (readByte() == 0x00.toByte()) MemoryInstruction.Size
        else throwException("Invalid index for memory.size (zero flag expected)", -1)
    }
    0x40 -> {
        if (readByte() == 0x00.toByte()) MemoryInstruction.Grow
        else throwException("Invalid index for memory.grow (zero flag expected)", -1)
    }
    0x28 -> MemoryInstruction.LoadInt.I32_LOAD.copy(arg = readMemoryArg())
    0x29 -> MemoryInstruction.LoadInt.I64_LOAD.copy(arg = readMemoryArg())
    0x2A -> MemoryInstruction.LoadFloat.F32_LOAD.copy(arg = readMemoryArg())
    0x2B -> MemoryInstruction.LoadFloat.F64_LOAD.copy(arg = readMemoryArg())
    0x2C -> MemoryInstruction.LoadInt.I32_LOAD8_S.copy(arg = readMemoryArg())
    0x2D -> MemoryInstruction.LoadInt.I32_LOAD8_U.copy(arg = readMemoryArg())
    0x2E -> MemoryInstruction.LoadInt.I32_LOAD16_S.copy(arg = readMemoryArg())
    0x2F -> MemoryInstruction.LoadInt.I32_LOAD16_U.copy(arg = readMemoryArg())
    0x30 -> MemoryInstruction.LoadInt.I64_LOAD8_S.copy(arg = readMemoryArg())
    0x31 -> MemoryInstruction.LoadInt.I64_LOAD8_U.copy(arg = readMemoryArg())
    0x32 -> MemoryInstruction.LoadInt.I64_LOAD16_S.copy(arg = readMemoryArg())
    0x33 -> MemoryInstruction.LoadInt.I64_LOAD16_U.copy(arg = readMemoryArg())
    0x34 -> MemoryInstruction.LoadInt.I64_LOAD32_S.copy(arg = readMemoryArg())
    0x35 -> MemoryInstruction.LoadInt.I64_LOAD32_U.copy(arg = readMemoryArg())
    0x36 -> MemoryInstruction.StoreInt.I32_STORE.copy(arg = readMemoryArg())
    0x37 -> MemoryInstruction.StoreInt.I64_STORE.copy(arg = readMemoryArg())
    0x38 -> MemoryInstruction.StoreFloat.F32_STORE.copy(arg = readMemoryArg())
    0x39 -> MemoryInstruction.StoreFloat.F64_STORE.copy(arg = readMemoryArg())
    0x3A -> MemoryInstruction.StoreInt.I32_STORE8.copy(arg = readMemoryArg())
    0x3B -> MemoryInstruction.StoreInt.I32_STORE16.copy(arg = readMemoryArg())
    0x3C -> MemoryInstruction.StoreInt.I64_STORE8.copy(arg = readMemoryArg())
    0x3D -> MemoryInstruction.StoreInt.I64_STORE16.copy(arg = readMemoryArg())
    0x3E -> MemoryInstruction.StoreInt.I64_STORE32.copy(arg = readMemoryArg())
    else -> throwException("Invalid Memory Instruction Opcode", -1)
}

/** Reads a [MemArg] node from the binary stream. */
fun BinaryParser.readMemoryArg(): MemArg = MemArg(alignment = readUInt(), offset = readUInt())
