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
import kwasm.ast.Identifier
import kwasm.ast.instruction.Instruction
import kwasm.ast.instruction.VariableInstruction
import kwasm.ast.module.Index
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayInputStream

@Suppress("UNCHECKED_CAST")
@RunWith(Parameterized::class)
class VariableInstructionTest(val param: Params) {
    @Test
    fun runTest() {
        val parser = BinaryParser(ByteArrayInputStream(param.bytes.toByteArray()))
        if (param.expectedError != null) {
            assertThrows(param.expectedError.javaClass) { parser.readInstruction() }
        } else {
            val actual = parser.readInstruction()
            assertThat(actual).isEqualTo(param.expected)
        }
    }

    data class Params(
        val bytes: List<Int>,
        val expected: Instruction?,
        val expectedError: Throwable? = null
    ) {
        override fun toString(): String =
            "${bytes.joinToString(" ") { "0x" + it.toString(16) }}: $expected"
    }

    companion object {
        @get:Parameterized.Parameters(name = "{0}")
        @get:JvmStatic
        val params = arrayOf(
            Params(
                listOf(0x20, 0x01),
                VariableInstruction.LocalGet(Index.ByInt(1) as Index<Identifier.Local>)
            ),
            Params(
                listOf(0x20, 0x7F), // Ensure unsigned
                VariableInstruction.LocalGet(Index.ByInt(127) as Index<Identifier.Local>)
            ),
            Params(
                listOf(0x21, 0x01),
                VariableInstruction.LocalSet(Index.ByInt(1) as Index<Identifier.Local>)
            ),
            Params(
                listOf(0x21, 0x7F), // Ensure unsigned
                VariableInstruction.LocalSet(Index.ByInt(127) as Index<Identifier.Local>)
            ),
            Params(
                listOf(0x22, 0x01),
                VariableInstruction.LocalTee(Index.ByInt(1) as Index<Identifier.Local>)
            ),
            Params(
                listOf(0x22, 0x7F), // Ensure unsigned
                VariableInstruction.LocalTee(Index.ByInt(127) as Index<Identifier.Local>)
            ),
            Params(
                listOf(0x23, 0x01),
                VariableInstruction.GlobalGet(Index.ByInt(1) as Index<Identifier.Global>)
            ),
            Params(
                listOf(0x23, 0x7F), // Ensure unsigned
                VariableInstruction.GlobalGet(Index.ByInt(127) as Index<Identifier.Global>)
            ),
            Params(
                listOf(0x24, 0x01),
                VariableInstruction.GlobalSet(Index.ByInt(1) as Index<Identifier.Global>)
            ),
            Params(
                listOf(0x24, 0x7F), // Ensure unsigned
                VariableInstruction.GlobalSet(Index.ByInt(127) as Index<Identifier.Global>)
            ),
            Params(
                listOf(0xFF, 0x07),
                expected = null,
                expectedError = ParseException(
                    "No instruction defined",
                    ParseContext("unknown.wasm", 0, 0)
                )
            )
        )
    }
}
