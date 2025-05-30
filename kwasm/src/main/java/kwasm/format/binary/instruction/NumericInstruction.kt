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

import kwasm.ast.FloatLiteral
import kwasm.ast.IntegerLiteral
import kwasm.ast.instruction.Instruction
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.instruction.NumericInstruction
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readDouble
import kwasm.format.binary.value.readFloat
import kwasm.format.binary.value.readInt
import kwasm.format.binary.value.readLong
import kwasm.format.binary.value.readUInt

internal val NUMERIC_OPCODE_RANGE = 0x41.toUByte().toInt()..0xC4.toUByte().toInt()
internal const val NUMERIC_SATURATING_TRUNCATION_OPCODE = 0xFC

/**
 * From
 * [the docs](https://webassembly.github.io/spec/core/binary/instructions.html#numeric-instructions)
 * :
 *
 * All variants of numeric instructions are represented by separate byte codes.
 *
 * The `const` instructions are followed by the respective literal.
 *
 * ```
 *      instr   ::= 0x41 n:i32  =>  i32.const n
 *                  0x42 n:i64  =>  i64.const n
 *                  0x43 z:f32  =>  f32.const z
 *                  0x44 z:f64  =>  f64.const z
 * ```
 *
 * All other numeric instructions are plain opcodes without any immediates.
 *
 * ```
 *      instr   ::= 0x45    => i32.eqz
 *                  0x46    => i32.eq
 *                  0x47    => i32.ne
 *                  0x48    => i32.lt_s
 *                  0x49    => i32.lt_u
 *                  0x4A    => i32.gt_s
 *                  0x4B    => i32.gt_u
 *                  0x4C    => i32.le_s
 *                  0x4D    => i32.le_u
 *                  0x4E    => i32.ge_s
 *                  0x4F    => i32.ge_u
 *
 *                  0x50    => i64.eqz
 *                  0x51    => i64.eq
 *                  0x52    => i64.ne
 *                  0x53    => i64.lt_s
 *                  0x54    => i64.lt_u
 *                  0x55    => i64.gt_s
 *                  0x56    => i64.gt_u
 *                  0x57    => i64.le_s
 *                  0x58    => i64.le_u
 *                  0x59    => i64.ge_s
 *                  0x5A    => i64.ge_u
 *
 *                  0x5B    => f32.eq
 *                  0x5C    => f32.ne
 *                  0x5D    => f32.lt
 *                  0x5E    => f32.gt
 *                  0x5F    => f32.le
 *                  0x60    => f32.ge
 *
 *                  0x61    => f64.eq
 *                  0x62    => f64.ne
 *                  0x63    => f64.lt
 *                  0x64    => f64.gt
 *                  0x65    => f64.le
 *                  0x66    => f64.ge
 *
 *                  0x67    => i32.clz
 *                  0x68    => i32.ctz
 *                  0x69    => i32.popcnt
 *                  0x6A    => i32.add
 *                  0x6B    => i32.sub
 *                  0x6C    => i32.mul
 *                  0x6D    => i32.div_s
 *                  0x6E    => i32.div_u
 *                  0x6F    => i32.rem_s
 *                  0x70    => i32.rem_u
 *                  0x71    => i32.and
 *                  0x72    => i32.or
 *                  0x73    => i32.xor
 *                  0x74    => i32.shl
 *                  0x75    => i32.shr_s
 *                  0x76    => i32.shr_u
 *                  0x77    => i32.rotl
 *                  0x78    => i32.rotr
 *
 *                  0x79    => i64.clz
 *                  0x7A    => i64.ctz
 *                  0x7B    => i64.popcnt
 *                  0x7C    => i64.add
 *                  0x7D    => i64.sub
 *                  0x7E    => i64.mul
 *                  0x7F    => i64.div_s
 *                  0x80    => i64.div_u
 *                  0x81    => i64.rem_s
 *                  0x82    => i64.rem_u
 *                  0x83    => i64.and
 *                  0x84    => i64.or
 *                  0x85    => i64.xor
 *                  0x86    => i64.shl
 *                  0x87    => i64.shr_s
 *                  0x88    => i64.shr_u
 *                  0x89    => i64.rotl
 *                  0x8A    => i64.rotr
 *
 *                  0x8B    => f32.abs
 *                  0x8C    => f32.neg
 *                  0x8D    => f32.ceil
 *                  0x8E    => f32.floor
 *                  0x8F    => f32.trunc
 *                  0x90    => f32.nearest
 *                  0x91    => f32.sqrt
 *                  0x92    => f32.add
 *                  0x93    => f32.sub
 *                  0x94    => f32.mul
 *                  0x95    => f32.div
 *                  0x96    => f32.min
 *                  0x97    => f32.max
 *                  0x98    => f32.copysign
 *
 *                  0x99    => f64.abs
 *                  0x9A    => f64.neg
 *                  0x9B    => f64.ceil
 *                  0x9C    => f64.floor
 *                  0x9D    => f64.trunc
 *                  0x9E    => f64.nearest
 *                  0x9F    => f64.sqrt
 *                  0xA0    => f64.add
 *                  0xA1    => f64.sub
 *                  0xA2    => f64.mul
 *                  0xA3    => f64.div
 *                  0xA4    => f64.min
 *                  0xA5    => f64.max
 *                  0xA6    => f64.copysign
 *
 *                  0xA7    => i32.wrap_i64
 *                  0xA8    => i32.trunc_f32_s
 *                  0xA9    => i32.trunc_f32_u
 *                  0xAA    => i32.trunc_f64_s
 *                  0xAB    => i32.trunc_f64_u
 *
 *                  0xAC    => i64.extend_i32_s
 *                  0xAD    => i64.extend_i32_u
 *                  0xAE    => i64.trunc_f32_s
 *                  0xAF    => i64.trunc_f32_u
 *                  0xB0    => i64.trunc_f64_s
 *                  0xB1    => i64.trunc_f64_u
 *
 *                  0xB2    => f32.convert_i32_s
 *                  0xB3    => f32.convert_i32_u
 *                  0xB4    => f32.convert_i64_s
 *                  0xB5    => f32.convert_i64_u
 *                  0xB6    => f32.demote_f64
 *
 *                  0xB7    => f64.convert_i32_s
 *                  0xB8    => f64.convert_i32_u
 *                  0xB9    => f64.convert_i64_s
 *                  0xBA    => f64.convert_i64_u
 *                  0xBB    => f64.promote_f32
 *
 *                  0xBC    => i32.reinterpret_f32
 *                  0xBD    => i64.reinterpret_f64
 *                  0xBE    => f32.reinterpret_i32
 *                  0xBF    => f64.reinterpret_i64
 *
 *                  0xC0    => i32.extend8_s
 *                  0xC1    => i32.extend16_s
 *                  0xC2    => i64.extend8_s
 *                  0xC3    => i64.extend16_s
 *                  0xC4    => i64.extend32_s
 * ```
 *
 * The saturating truncation instructions all have a one byte prefix, whereas the actual opcode is encoded by a variable-length unsigned integer.
 *
 * ```
 *      instr   ::=     0xFC 0:u32  => i32.trunc_sat_f32_s
 *                      0xFC 1:u32  => i32.trunc_sat_f32_u
 *                      0xFC 2:u32  => i32.trunc_sat_f64_s
 *                      0xFC 3:u32  => i32.trunc_sat_f64_u
 *                      0xFC 4:u32  => i64.trunc_sat_f32_s
 *                      0xFC 5:u32  => i64.trunc_sat_f32_u
 *                      0xFC 6:u32  => i64.trunc_sat_f64_s
 *                      0xFC 7:u32  => i64.trunc_sat_f64_u
 * ```
 */
fun BinaryParser.readNumericInstruction(opcode: Int): Instruction {
    return when (opcode) {
        0x41 -> NumericConstantInstruction.I32(IntegerLiteral.S32(readInt()))
        0x42 -> NumericConstantInstruction.I64(IntegerLiteral.S64(readLong()))
        0x43 -> NumericConstantInstruction.F32(FloatLiteral.SinglePrecision(readFloat()))
        0x44 -> NumericConstantInstruction.F64(FloatLiteral.DoublePrecision(readDouble()))
        0x45 -> NumericInstruction.I32EqualsZero
        0x46 -> NumericInstruction.I32Equals
        0x47 -> NumericInstruction.I32NotEquals
        0x48 -> NumericInstruction.I32LessThanSigned
        0x49 -> NumericInstruction.I32LessThanUnsigned
        0x4A -> NumericInstruction.I32GreaterThanSigned
        0x4B -> NumericInstruction.I32GreaterThanUnsigned
        0x4C -> NumericInstruction.I32LessThanEqualToSigned
        0x4D -> NumericInstruction.I32LessThanEqualToUnsigned
        0x4E -> NumericInstruction.I32GreaterThanEqualToSigned
        0x4F -> NumericInstruction.I32GreaterThanEqualToUnsigned
        0x50 -> NumericInstruction.I64EqualsZero
        0x51 -> NumericInstruction.I64Equals
        0x52 -> NumericInstruction.I64NotEquals
        0x53 -> NumericInstruction.I64LessThanSigned
        0x54 -> NumericInstruction.I64LessThanUnsigned
        0x55 -> NumericInstruction.I64GreaterThanSigned
        0x56 -> NumericInstruction.I64GreaterThanUnsigned
        0x57 -> NumericInstruction.I64LessThanEqualToSigned
        0x58 -> NumericInstruction.I64LessThanEqualToUnsigned
        0x59 -> NumericInstruction.I64GreaterThanEqualToSigned
        0x5A -> NumericInstruction.I64GreaterThanEqualToUnsigned
        0x5B -> NumericInstruction.F32Equals
        0x5C -> NumericInstruction.F32NotEquals
        0x5D -> NumericInstruction.F32LessThan
        0x5E -> NumericInstruction.F32GreaterThan
        0x5F -> NumericInstruction.F32LessThanEqualTo
        0x60 -> NumericInstruction.F32GreaterThanEqualTo
        0x61 -> NumericInstruction.F64Equals
        0x62 -> NumericInstruction.F64NotEquals
        0x63 -> NumericInstruction.F64LessThan
        0x64 -> NumericInstruction.F64GreaterThan
        0x65 -> NumericInstruction.F64LessThanEqualTo
        0x66 -> NumericInstruction.F64GreaterThanEqualTo
        0x67 -> NumericInstruction.I32CountLeadingZeroes
        0x68 -> NumericInstruction.I32CountTrailingZeroes
        0x69 -> NumericInstruction.I32CountNonZeroBits
        0x6A -> NumericInstruction.I32Add
        0x6B -> NumericInstruction.I32Subtract
        0x6C -> NumericInstruction.I32Multiply
        0x6D -> NumericInstruction.I32DivideSigned
        0x6E -> NumericInstruction.I32DivideUnsigned
        0x6F -> NumericInstruction.I32RemainderSigned
        0x70 -> NumericInstruction.I32RemainderUnsigned
        0x71 -> NumericInstruction.I32BitwiseAnd
        0x72 -> NumericInstruction.I32BitwiseOr
        0x73 -> NumericInstruction.I32BitwiseXor
        0x74 -> NumericInstruction.I32ShiftLeft
        0x75 -> NumericInstruction.I32ShiftRightSigned
        0x76 -> NumericInstruction.I32ShiftRightUnsigned
        0x77 -> NumericInstruction.I32RotateLeft
        0x78 -> NumericInstruction.I32RotateRight
        0x79 -> NumericInstruction.I64CountLeadingZeroes
        0x7A -> NumericInstruction.I64CountTrailingZeroes
        0x7B -> NumericInstruction.I64CountNonZeroBits
        0x7C -> NumericInstruction.I64Add
        0x7D -> NumericInstruction.I64Subtract
        0x7E -> NumericInstruction.I64Multiply
        0x7F -> NumericInstruction.I64DivideSigned
        0x80 -> NumericInstruction.I64DivideUnsigned
        0x81 -> NumericInstruction.I64RemainderSigned
        0x82 -> NumericInstruction.I64RemainderUnsigned
        0x83 -> NumericInstruction.I64BitwiseAnd
        0x84 -> NumericInstruction.I64BitwiseOr
        0x85 -> NumericInstruction.I64BitwiseXor
        0x86 -> NumericInstruction.I64ShiftLeft
        0x87 -> NumericInstruction.I64ShiftRightSigned
        0x88 -> NumericInstruction.I64ShiftRightUnsigned
        0x89 -> NumericInstruction.I64RotateLeft
        0x8A -> NumericInstruction.I64RotateRight
        0x8B -> NumericInstruction.F32AbsoluteValue
        0x8C -> NumericInstruction.F32Negative
        0x8D -> NumericInstruction.F32Ceiling
        0x8E -> NumericInstruction.F32Floor
        0x8F -> NumericInstruction.F32Truncate
        0x90 -> NumericInstruction.F32Nearest
        0x91 -> NumericInstruction.F32SquareRoot
        0x92 -> NumericInstruction.F32Add
        0x93 -> NumericInstruction.F32Subtract
        0x94 -> NumericInstruction.F32Multiply
        0x95 -> NumericInstruction.F32Divide
        0x96 -> NumericInstruction.F32Min
        0x97 -> NumericInstruction.F32Max
        0x98 -> NumericInstruction.F32CopySign
        0x99 -> NumericInstruction.F64AbsoluteValue
        0x9A -> NumericInstruction.F64Negative
        0x9B -> NumericInstruction.F64Ceiling
        0x9C -> NumericInstruction.F64Floor
        0x9D -> NumericInstruction.F64Truncate
        0x9E -> NumericInstruction.F64Nearest
        0x9F -> NumericInstruction.F64SquareRoot
        0xA0 -> NumericInstruction.F64Add
        0xA1 -> NumericInstruction.F64Subtract
        0xA2 -> NumericInstruction.F64Multiply
        0xA3 -> NumericInstruction.F64Divide
        0xA4 -> NumericInstruction.F64Min
        0xA5 -> NumericInstruction.F64Max
        0xA6 -> NumericInstruction.F64CopySign
        0xA7 -> NumericInstruction.I32WrapI64
        0xA8 -> NumericInstruction.I32TruncateF32Signed
        0xA9 -> NumericInstruction.I32TruncateF32Unsigned
        0xAA -> NumericInstruction.I32TruncateF64Signed
        0xAB -> NumericInstruction.I32TruncateF64Unsigned
        0xAC -> NumericInstruction.I64ExtendI32Signed
        0xAD -> NumericInstruction.I64ExtendI32Unsigned
        0xAE -> NumericInstruction.I64TruncateF32Signed
        0xAF -> NumericInstruction.I64TruncateF32Unsigned
        0xB0 -> NumericInstruction.I64TruncateF64Signed
        0xB1 -> NumericInstruction.I64TruncateF64Unsigned
        0xB2 -> NumericInstruction.F32ConvertI32Signed
        0xB3 -> NumericInstruction.F32ConvertI32Unsigned
        0xB4 -> NumericInstruction.F32ConvertI64Signed
        0xB5 -> NumericInstruction.F32ConvertI64Unsigned
        0xB6 -> NumericInstruction.F32DemoteF64
        0xB7 -> NumericInstruction.F64ConvertI32Signed
        0xB8 -> NumericInstruction.F64ConvertI32Unsigned
        0xB9 -> NumericInstruction.F64ConvertI64Signed
        0xBA -> NumericInstruction.F64ConvertI64Unsigned
        0xBB -> NumericInstruction.F64PromoteF32
        0xBC -> NumericInstruction.I32ReinterpretF32
        0xBD -> NumericInstruction.I64ReinterpretF64
        0xBE -> NumericInstruction.F32ReinterpretI32
        0xBF -> NumericInstruction.F64ReinterpretI64
        0xC0 -> NumericInstruction.I32Extend8Signed
        0xC1 -> NumericInstruction.I32Extend16Signed
        0xC2 -> NumericInstruction.I64Extend8Signed
        0xC3 -> NumericInstruction.I64Extend16Signed
        0xC4 -> NumericInstruction.I64Extend32Signed
        0xFC -> {
            when (readUInt()) {
                0 -> NumericInstruction.I32TruncateSaturatedF32Signed
                1 -> NumericInstruction.I32TruncateSaturatedF32Unsigned
                2 -> NumericInstruction.I32TruncateSaturatedF64Signed
                3 -> NumericInstruction.I32TruncateSaturatedF64Unsigned
                4 -> NumericInstruction.I64TruncateSaturatedF32Signed
                5 -> NumericInstruction.I64TruncateSaturatedF32Unsigned
                6 -> NumericInstruction.I64TruncateSaturatedF64Signed
                7 -> NumericInstruction.I64TruncateSaturatedF64Unsigned
                else -> throwException(
                    "Invalid mutex value for saturated truncation instruction",
                    -2
                )
            }
        }
        else -> throwException("Unknown numeric instruction", -1)
    }
}
