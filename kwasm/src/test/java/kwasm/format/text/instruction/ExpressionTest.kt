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

package kwasm.format.text.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.module.Index
import kwasm.format.ParseContext
import kwasm.format.text.Tokenizer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExpressionTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("ExpressionTest.wat")

    @Test
    fun parse_emptyExpressionList_fromEmptySource() {
        val result = tokenizer.tokenize("", context).parseExpression(0)
        assertThat(result.parseLength).isEqualTo(0)
        assertThat(result.astNode.instructions).isEmpty()
    }

    @Test
    fun parse_emptyExpressionList_nonExpression() {
        val result = tokenizer.tokenize("0x5", context).parseExpression(0)
        assertThat(result.parseLength).isEqualTo(0)
        assertThat(result.astNode.instructions).isEmpty()
    }

    @Test
    fun parse_singleInstruction() {
        val result = tokenizer.tokenize("return", context).parseExpression(0)
        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode.instructions).containsExactly(ControlInstruction.Return)
    }

    @Test
    fun parse_multipleInstructions() {
        val result = tokenizer.tokenize("return br $0").parseExpression(0)
        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode.instructions).containsExactly(
            ControlInstruction.Return,
            ControlInstruction.Break(Index.ByIdentifier(Identifier.Label("$0")))
        )
    }
}
