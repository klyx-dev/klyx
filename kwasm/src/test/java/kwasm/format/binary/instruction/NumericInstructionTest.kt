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

import com.google.common.truth.Truth.assertThat
import kwasm.ast.FloatLiteral
import kwasm.ast.IntegerLiteral
import kwasm.ast.instruction.Instruction
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.instruction.NumericInstruction
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayInputStream

@RunWith(Parameterized::class)
class NumericInstructionTest(val params: Params) {
    @Test
    fun testInstruction() {
        val byteArray = params.bytes.toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(byteArray))

        if (params.expected != null) {
            assertThat(parser.readInstruction()).isEqualTo(params.expected)
        } else if (params.error != null) {
            val e = assertThrows(params.error.javaClass) { parser.readInstruction() }
            assertThat(e).hasMessageThat().contains(params.error.message)
        }
    }

    data class Params(
        val bytes: List<Int>,
        val expected: Instruction?,
        val error: ParseException? = null
    ) {
        override fun toString(): String = "0x${bytes[0].toString(16)}: $expected"
    }

    companion object {
        @get:JvmStatic
        @get:Parameterized.Parameters(name = "{0}")
        val parameters = arrayOf(
            Params(
                listOf(0x41, 0x8F, 0x01),
                NumericConstantInstruction.I32(IntegerLiteral.S32(143))
            ),
            Params(
                listOf(0x42, 0xFF, 0x01),
                NumericConstantInstruction.I64(IntegerLiteral.S64(255))
            ),
            Params(
                listOf(0x43, 0x00, 0x00, 0x00, 0x00),
                NumericConstantInstruction.F32(FloatLiteral.SinglePrecision(0.0f))
            ),
            Params(
                listOf(0x44, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
                NumericConstantInstruction.F64(FloatLiteral.DoublePrecision(0.0))
            ),
            Params(listOf(0x45), NumericInstruction.I32EqualsZero),
            Params(listOf(0x46), NumericInstruction.I32Equals),
            Params(listOf(0x47), NumericInstruction.I32NotEquals),
            Params(listOf(0x48), NumericInstruction.I32LessThanSigned),
            Params(listOf(0x49), NumericInstruction.I32LessThanUnsigned),
            Params(listOf(0x4A), NumericInstruction.I32GreaterThanSigned),
            Params(listOf(0x4B), NumericInstruction.I32GreaterThanUnsigned),
            Params(listOf(0x4C), NumericInstruction.I32LessThanEqualToSigned),
            Params(listOf(0x4D), NumericInstruction.I32LessThanEqualToUnsigned),
            Params(listOf(0x4E), NumericInstruction.I32GreaterThanEqualToSigned),
            Params(listOf(0x4F), NumericInstruction.I32GreaterThanEqualToUnsigned),
            Params(listOf(0x50), NumericInstruction.I64EqualsZero),
            Params(listOf(0x51), NumericInstruction.I64Equals),
            Params(listOf(0x52), NumericInstruction.I64NotEquals),
            Params(listOf(0x53), NumericInstruction.I64LessThanSigned),
            Params(listOf(0x54), NumericInstruction.I64LessThanUnsigned),
            Params(listOf(0x55), NumericInstruction.I64GreaterThanSigned),
            Params(listOf(0x56), NumericInstruction.I64GreaterThanUnsigned),
            Params(listOf(0x57), NumericInstruction.I64LessThanEqualToSigned),
            Params(listOf(0x58), NumericInstruction.I64LessThanEqualToUnsigned),
            Params(listOf(0x59), NumericInstruction.I64GreaterThanEqualToSigned),
            Params(listOf(0x5A), NumericInstruction.I64GreaterThanEqualToUnsigned),
            Params(listOf(0x5B), NumericInstruction.F32Equals),
            Params(listOf(0x5C), NumericInstruction.F32NotEquals),
            Params(listOf(0x5D), NumericInstruction.F32LessThan),
            Params(listOf(0x5E), NumericInstruction.F32GreaterThan),
            Params(listOf(0x5F), NumericInstruction.F32LessThanEqualTo),
            Params(listOf(0x60), NumericInstruction.F32GreaterThanEqualTo),
            Params(listOf(0x61), NumericInstruction.F64Equals),
            Params(listOf(0x62), NumericInstruction.F64NotEquals),
            Params(listOf(0x63), NumericInstruction.F64LessThan),
            Params(listOf(0x64), NumericInstruction.F64GreaterThan),
            Params(listOf(0x65), NumericInstruction.F64LessThanEqualTo),
            Params(listOf(0x66), NumericInstruction.F64GreaterThanEqualTo),
            Params(listOf(0x67), NumericInstruction.I32CountLeadingZeroes),
            Params(listOf(0x68), NumericInstruction.I32CountTrailingZeroes),
            Params(listOf(0x69), NumericInstruction.I32CountNonZeroBits),
            Params(listOf(0x6A), NumericInstruction.I32Add),
            Params(listOf(0x6B), NumericInstruction.I32Subtract),
            Params(listOf(0x6C), NumericInstruction.I32Multiply),
            Params(listOf(0x6D), NumericInstruction.I32DivideSigned),
            Params(listOf(0x6E), NumericInstruction.I32DivideUnsigned),
            Params(listOf(0x6F), NumericInstruction.I32RemainderSigned),
            Params(listOf(0x70), NumericInstruction.I32RemainderUnsigned),
            Params(listOf(0x71), NumericInstruction.I32BitwiseAnd),
            Params(listOf(0x72), NumericInstruction.I32BitwiseOr),
            Params(listOf(0x73), NumericInstruction.I32BitwiseXor),
            Params(listOf(0x74), NumericInstruction.I32ShiftLeft),
            Params(listOf(0x75), NumericInstruction.I32ShiftRightSigned),
            Params(listOf(0x76), NumericInstruction.I32ShiftRightUnsigned),
            Params(listOf(0x77), NumericInstruction.I32RotateLeft),
            Params(listOf(0x78), NumericInstruction.I32RotateRight),
            Params(listOf(0x79), NumericInstruction.I64CountLeadingZeroes),
            Params(listOf(0x7A), NumericInstruction.I64CountTrailingZeroes),
            Params(listOf(0x7B), NumericInstruction.I64CountNonZeroBits),
            Params(listOf(0x7C), NumericInstruction.I64Add),
            Params(listOf(0x7D), NumericInstruction.I64Subtract),
            Params(listOf(0x7E), NumericInstruction.I64Multiply),
            Params(listOf(0x7F), NumericInstruction.I64DivideSigned),
            Params(listOf(0x80), NumericInstruction.I64DivideUnsigned),
            Params(listOf(0x81), NumericInstruction.I64RemainderSigned),
            Params(listOf(0x82), NumericInstruction.I64RemainderUnsigned),
            Params(listOf(0x83), NumericInstruction.I64BitwiseAnd),
            Params(listOf(0x84), NumericInstruction.I64BitwiseOr),
            Params(listOf(0x85), NumericInstruction.I64BitwiseXor),
            Params(listOf(0x86), NumericInstruction.I64ShiftLeft),
            Params(listOf(0x87), NumericInstruction.I64ShiftRightSigned),
            Params(listOf(0x88), NumericInstruction.I64ShiftRightUnsigned),
            Params(listOf(0x89), NumericInstruction.I64RotateLeft),
            Params(listOf(0x8A), NumericInstruction.I64RotateRight),
            Params(listOf(0x8B), NumericInstruction.F32AbsoluteValue),
            Params(listOf(0x8C), NumericInstruction.F32Negative),
            Params(listOf(0x8D), NumericInstruction.F32Ceiling),
            Params(listOf(0x8E), NumericInstruction.F32Floor),
            Params(listOf(0x8F), NumericInstruction.F32Truncate),
            Params(listOf(0x90), NumericInstruction.F32Nearest),
            Params(listOf(0x91), NumericInstruction.F32SquareRoot),
            Params(listOf(0x92), NumericInstruction.F32Add),
            Params(listOf(0x93), NumericInstruction.F32Subtract),
            Params(listOf(0x94), NumericInstruction.F32Multiply),
            Params(listOf(0x95), NumericInstruction.F32Divide),
            Params(listOf(0x96), NumericInstruction.F32Min),
            Params(listOf(0x97), NumericInstruction.F32Max),
            Params(listOf(0x98), NumericInstruction.F32CopySign),
            Params(listOf(0x99), NumericInstruction.F64AbsoluteValue),
            Params(listOf(0x9A), NumericInstruction.F64Negative),
            Params(listOf(0x9B), NumericInstruction.F64Ceiling),
            Params(listOf(0x9C), NumericInstruction.F64Floor),
            Params(listOf(0x9D), NumericInstruction.F64Truncate),
            Params(listOf(0x9E), NumericInstruction.F64Nearest),
            Params(listOf(0x9F), NumericInstruction.F64SquareRoot),
            Params(listOf(0xA0), NumericInstruction.F64Add),
            Params(listOf(0xA1), NumericInstruction.F64Subtract),
            Params(listOf(0xA2), NumericInstruction.F64Multiply),
            Params(listOf(0xA3), NumericInstruction.F64Divide),
            Params(listOf(0xA4), NumericInstruction.F64Min),
            Params(listOf(0xA5), NumericInstruction.F64Max),
            Params(listOf(0xA6), NumericInstruction.F64CopySign),
            Params(listOf(0xA7), NumericInstruction.I32WrapI64),
            Params(listOf(0xA8), NumericInstruction.I32TruncateF32Signed),
            Params(listOf(0xA9), NumericInstruction.I32TruncateF32Unsigned),
            Params(listOf(0xAA), NumericInstruction.I32TruncateF64Signed),
            Params(listOf(0xAB), NumericInstruction.I32TruncateF64Unsigned),
            Params(listOf(0xAC), NumericInstruction.I64ExtendI32Signed),
            Params(listOf(0xAD), NumericInstruction.I64ExtendI32Unsigned),
            Params(listOf(0xAE), NumericInstruction.I64TruncateF32Signed),
            Params(listOf(0xAF), NumericInstruction.I64TruncateF32Unsigned),
            Params(listOf(0xB0), NumericInstruction.I64TruncateF64Signed),
            Params(listOf(0xB1), NumericInstruction.I64TruncateF64Unsigned),
            Params(listOf(0xB2), NumericInstruction.F32ConvertI32Signed),
            Params(listOf(0xB3), NumericInstruction.F32ConvertI32Unsigned),
            Params(listOf(0xB4), NumericInstruction.F32ConvertI64Signed),
            Params(listOf(0xB5), NumericInstruction.F32ConvertI64Unsigned),
            Params(listOf(0xB6), NumericInstruction.F32DemoteF64),
            Params(listOf(0xB7), NumericInstruction.F64ConvertI32Signed),
            Params(listOf(0xB8), NumericInstruction.F64ConvertI32Unsigned),
            Params(listOf(0xB9), NumericInstruction.F64ConvertI64Signed),
            Params(listOf(0xBA), NumericInstruction.F64ConvertI64Unsigned),
            Params(listOf(0xBB), NumericInstruction.F64PromoteF32),
            Params(listOf(0xBC), NumericInstruction.I32ReinterpretF32),
            Params(listOf(0xBD), NumericInstruction.I64ReinterpretF64),
            Params(listOf(0xBE), NumericInstruction.F32ReinterpretI32),
            Params(listOf(0xBF), NumericInstruction.F64ReinterpretI64),
            Params(listOf(0xC0), NumericInstruction.I32Extend8Signed),
            Params(listOf(0xC1), NumericInstruction.I32Extend16Signed),
            Params(listOf(0xC2), NumericInstruction.I64Extend8Signed),
            Params(listOf(0xC3), NumericInstruction.I64Extend16Signed),
            Params(listOf(0xC4), NumericInstruction.I64Extend32Signed),
            Params(listOf(0xFC, 0x00), NumericInstruction.I32TruncateSaturatedF32Signed),
            Params(listOf(0xFC, 0x01), NumericInstruction.I32TruncateSaturatedF32Unsigned),
            Params(listOf(0xFC, 0x02), NumericInstruction.I32TruncateSaturatedF64Signed),
            Params(listOf(0xFC, 0x03), NumericInstruction.I32TruncateSaturatedF64Unsigned),
            Params(listOf(0xFC, 0x04), NumericInstruction.I64TruncateSaturatedF32Signed),
            Params(listOf(0xFC, 0x05), NumericInstruction.I64TruncateSaturatedF32Unsigned),
            Params(listOf(0xFC, 0x06), NumericInstruction.I64TruncateSaturatedF64Signed),
            Params(listOf(0xFC, 0x07), NumericInstruction.I64TruncateSaturatedF64Unsigned),
            Params(
                listOf(0xFC, 0x08),
                null,
                ParseException("Invalid mutex value", ParseContext("unknown.wasm", 1, 0))
            ),
            Params(
                listOf(0xC5),
                null,
                ParseException("", ParseContext("unknown.wasm", 1, 0))
            ),
        )
    }
}
