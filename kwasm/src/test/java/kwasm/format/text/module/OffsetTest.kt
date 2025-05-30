/*
 * Copyright 2020 Google LLC
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

package kwasm.format.text.module

import com.google.common.truth.Truth.assertThat
import kwasm.ast.IntegerLiteral
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.instruction.NumericInstruction
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OffsetTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("OffsetTest.wast")

    @Test
    fun throws_ifAbbreviated_containsNoInstructions() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("notAnInstruction", context).parseOffset(0)
        }
        assertThat(e).hasMessageThat()
            .contains("Expected offset expression")
    }

    @Test
    fun parses_abbreviatedFormat() {
        val result = tokenizer.tokenize("i32.const 1", context).parseOffset(0)
        assertThat(result.parseLength).isEqualTo(2)
        assertThat(result.astNode.expression.instructions).containsExactly(
            NumericConstantInstruction.I32(IntegerLiteral.S32(1))
        )
    }

    @Test
    fun throws_ifFull_hasMoreInstructionsThanRequested() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(offset i32.const 1 i32.const 2 i32.const 3)", context)
                .parseOffset(0, maxExpressionLength = 2)
        }
        assertThat(e).hasMessageThat()
            .contains("Expected at most 2 instructions for offset expression")
    }

    @Test
    fun throws_ifFull_isNotClosed() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(offset i32.const 1", context).parseOffset(0)
        }
        assertThat(e).hasMessageThat()
            .contains("Expected ')'")
    }

    @Test
    fun parses_fullFormat() {
        val result = tokenizer.tokenize("(offset i32.const 1 i32.const 2 i32.add)", context)
            .parseOffset(0)
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode.expression.instructions).containsExactly(
            NumericConstantInstruction.I32(IntegerLiteral.S32(1)),
            NumericConstantInstruction.I32(IntegerLiteral.S32(2)),
            NumericInstruction.I32Add
        )
    }
}
