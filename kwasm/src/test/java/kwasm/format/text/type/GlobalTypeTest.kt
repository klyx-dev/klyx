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
import kwasm.ast.type.GlobalType
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
class GlobalTypeTest {

    private val context = ParseContext("GlobalTypeTest.wasm", 1, 1)
    private val tokenizer = Tokenizer()

    @Test
    fun parseValidGlobalType_NonMutable() {
        val expectedValuetype = ParseResult(
            GlobalType(
                ValueType.I32,
                false
            ),
            1
        )
        val actual = tokenizer.tokenize("i32", context).parseGlobalType(0)
        assertThat(actual).isEqualTo(expectedValuetype)
    }

    @Test
    fun parseValidResultType_Mutable() {
        val expectedValuetype = ParseResult(
            GlobalType(
                ValueType.I32,
                true
            ),
            4
        )
        val actual = tokenizer.tokenize("(mut i32)", context).parseGlobalType(0)
        assertThat(actual).isEqualTo(expectedValuetype)
    }

    @Test
    fun parseInvalidResultType_DifferentFunction() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(foo bar)", context).parseGlobalType(0)
        }
        assertThat(exception).hasMessageThat().contains("Invalid GlobalType: Expecting \"mut\"")
    }

    @Test
    fun parseInvalidResultType_BadValueType() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(mut blah)", context).parseGlobalType(0)
        }
        assertThat(exception).hasMessageThat().contains("Invalid ValueType: Expecting i32, i64, f32, or f64")
    }
}
