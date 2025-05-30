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

package kwasm.format.binary.module

import com.google.common.truth.Truth.assertThat
import kwasm.ast.IntegerLiteral
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.module.Local
import kwasm.ast.type.ValueType
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class CodeSectionTest {
    @Test
    fun readCodeSection_empty() {
        val bytes = listOf(0x0A, 0x01, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isEqualTo(CodeSection(emptyList()))
    }

    @Test
    fun readCodeSection_nonEmpty() {
        val bytes = listOf(
            0x0A,
            0x0D,
            0x02,
            0x04,
            0x00, 0x41, 0x00, 0x0B,
            0x06,
            0x01, 0x02, 0x7F, 0x41, 0x00, 0x0B
        ).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isEqualTo(
            CodeSection(
                listOf(
                    Func(
                        locals = emptyList(),
                        expr = Expression(
                            listOf(
                                NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                            )
                        )
                    ),
                    Func(
                        locals = listOf(
                            Local(null, ValueType.I32),
                            Local(null, ValueType.I32),
                        ),
                        expr = Expression(
                            listOf(
                                NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun readCode_invalidSize_tooShort_throws() {
        val bytes = listOf(0x01, 0x00, 0x0B).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val e = assertThrows(ParseException::class.java) { parser.readCode() }
        assertThat(e).hasMessageThat()
            .contains("Code section func body does not match declared length")
    }

    @Test
    fun readCode_invalidSize_tooLong_throws() {
        val bytes = listOf(0x7F, 0x00, 0x41, 0x00, 0x0B).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val e = assertThrows(ParseException::class.java) { parser.readCode() }
        assertThat(e).hasMessageThat()
            .contains("Code section func body does not match declared length")
    }

    @Test
    fun readCode_emptyLocals() {
        val bytes = listOf(0x04, 0x00, 0x41, 0x00, 0x0B).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val expected = Func(
            locals = emptyList(),
            expr = Expression(
                listOf(
                    NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                )
            )
        )

        assertThat(parser.readCode()).isEqualTo(expected)
    }

    @Test
    fun readCode_nonEmptyLocals() {
        val bytes = listOf(0x06, 0x01, 0x02, 0x7F, 0x41, 0x00, 0x0B).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val expected = Func(
            locals = listOf(
                Local(null, ValueType.I32),
                Local(null, ValueType.I32),
            ),
            expr = Expression(
                listOf(
                    NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                )
            )
        )

        assertThat(parser.readCode()).isEqualTo(expected)
    }
}
