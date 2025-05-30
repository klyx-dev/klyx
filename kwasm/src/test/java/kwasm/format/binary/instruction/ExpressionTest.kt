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
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.NumericInstruction
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class ExpressionTest {
    @Test
    fun parseEmpty() {
        val parser = BinaryParser(ByteArrayInputStream(listOf(0x0B).toByteArray()))
        assertThat(parser.readExpression()).isEqualTo(Expression(emptyList()))
    }

    @Test
    fun parseSingleInstruction() {
        val parser = BinaryParser(ByteArrayInputStream(listOf(0x6A, 0x0B).toByteArray()))
        assertThat(parser.readExpression()).isEqualTo(Expression(listOf(NumericInstruction.I32Add)))
    }

    @Test
    fun parseMultipleInstructions() {
        val parser = BinaryParser(
            ByteArrayInputStream(listOf(0x6A, 0x6B, 0x6C, 0x0B).toByteArray())
        )
        assertThat(parser.readExpression())
            .isEqualTo(
                Expression(
                    listOf(
                        NumericInstruction.I32Add,
                        NumericInstruction.I32Subtract,
                        NumericInstruction.I32Multiply
                    )
                )
            )
    }

    @Test
    fun noEnd_throws() {
        val parser = BinaryParser(
            ByteArrayInputStream(listOf(0x6A).toByteArray())
        )
        assertThrows(ParseException::class.java) { parser.readExpression() }
    }
}
