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

import com.google.common.truth.Truth
import kwasm.ast.instruction.ParametricInstruction
import kwasm.format.ParseContext
import kwasm.format.text.ParseResult
import kwasm.format.text.Tokenizer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ParametricInstructionTest(private val testCase: Pair<String, ParametricInstruction>) {
    @Test
    fun parsesInstruction() {
        val tokenizer = Tokenizer()
        val context = ParseContext("${testCase.first}.wat")
        Truth.assertThat(tokenizer.tokenize(testCase.first, context).parseParametricInstruction(0))
            .isEqualTo(ParseResult(testCase.second, 1))
    }

    @Test
    fun parsesInstruction_fromParseInstruction() {
        val tokenizer = Tokenizer()
        val context = ParseContext("${testCase.first}.wat")
        Truth.assertThat(tokenizer.tokenize(testCase.first, context).parseInstruction(0))
            .isEqualTo(ParseResult(testCase.second, 1))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): List<Pair<String, ParametricInstruction>> = listOf(
            "drop" to ParametricInstruction.Drop,
            "select" to ParametricInstruction.Select
        )
    }
}
