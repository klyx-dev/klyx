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

package kwasm.format.text.type

import com.google.common.truth.Truth.assertThat
import kwasm.ast.type.Result
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.Tokenizer
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ResultTypeTest {

    private val context = ParseContext("ResultTypeTest.wasm", 1, 1)
    private val tokenizer = Tokenizer()

    @Test
    fun parseValidResultType_Exists() {
        val expected = ParseResult(
            ResultType(Result(ValueType.I32)),
            4
        )
        val actual = tokenizer.tokenize("(result i32)", context).parseResultType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidResultType_DifferentFunction() {
        val expected = ParseResult(ResultType(null), 0)
        val actual = tokenizer.tokenize("(foo bar)", context).parseResultType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidResultType_BadValueType() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(result blah)", context).parseResultType(0)
        }
    }
}
