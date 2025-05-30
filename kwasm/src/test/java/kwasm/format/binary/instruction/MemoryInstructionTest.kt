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
import kwasm.ast.instruction.MemArg
import kwasm.ast.instruction.MemoryInstruction
import kwasm.ast.instruction.MemoryInstruction.LoadFloat
import kwasm.ast.instruction.MemoryInstruction.LoadInt
import kwasm.ast.instruction.MemoryInstruction.StoreFloat
import kwasm.ast.instruction.MemoryInstruction.StoreInt
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
class MemoryInstructionTest(val param: Params) {
    @Test
    fun runTest() {
        val bytes = (listOf(param.opcode) + param.otherBytes).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        if (param.expectedException != null) {
            val e = assertThrows(param.expectedException.javaClass) {
                parser.readInstruction()
            }
            assertThat(e).hasMessageThat().contains(param.expectedException.message)
        } else {
            val ins = parser.readInstruction()
            assertThat(ins).isEqualTo(param.expectedInstruction)
        }
    }

    data class Params(
        val opcode: Int,
        val otherBytes: List<Int>,
        val expectedInstruction: MemoryInstruction?,
        val expectedException: Throwable? = null
    ) {
        override fun toString(): String =
            "0x${opcode.toString(16)}: $expectedInstruction / $expectedException"
    }

    companion object {
        @get:Parameterized.Parameters(name = "{0}")
        @get:JvmStatic
        val parameters = arrayOf(
            Params(
                opcode = 0x28,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I32_LOAD.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x29,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I64_LOAD.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x2A,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadFloat.F32_LOAD.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x2B,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadFloat.F64_LOAD.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x2C,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I32_LOAD8_S.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x2D,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I32_LOAD8_U.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x2E,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I32_LOAD16_S.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x2F,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I32_LOAD16_U.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x30,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I64_LOAD8_S.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x31,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I64_LOAD8_U.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x32,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I64_LOAD16_S.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x33,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I64_LOAD16_U.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x34,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I64_LOAD32_S.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x35,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = LoadInt.I64_LOAD32_U.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x36,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = StoreInt.I32_STORE.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x37,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = StoreInt.I64_STORE.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x38,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = StoreFloat.F32_STORE.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x39,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = StoreFloat.F64_STORE.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x3A,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = StoreInt.I32_STORE8.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x3B,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = StoreInt.I32_STORE16.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x3C,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = StoreInt.I64_STORE8.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x3D,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = StoreInt.I64_STORE16.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x3E,
                otherBytes = listOf(0xFF, 0x01, 0xFF, 0x01),
                expectedInstruction = StoreInt.I64_STORE32.copy(arg = MemArg(255, 255))
            ),
            Params(
                opcode = 0x3F,
                otherBytes = listOf(0x00),
                expectedInstruction = MemoryInstruction.Size
            ),
            Params(
                opcode = 0x40,
                otherBytes = listOf(0x00),
                expectedInstruction = MemoryInstruction.Grow
            ),
            Params(
                opcode = 0xFF,
                otherBytes = listOf(0x00),
                expectedInstruction = null,
                expectedException = ParseException(
                    "No instruction defined",
                    ParseContext("unknown.wasm", 1, 0)
                )
            ),
            Params(
                opcode = 0x3F,
                otherBytes = listOf(0x01),
                expectedInstruction = null,
                expectedException = ParseException(
                    "Invalid index for memory.size",
                    ParseContext("unknown.wasm", 1, 1)
                )
            ),
            Params(
                opcode = 0x40,
                otherBytes = listOf(0x01),
                expectedInstruction = null,
                expectedException = ParseException(
                    "Invalid index for memory.grow",
                    ParseContext("unknown.wasm", 1, 1)
                )
            ),
        )
    }
}
