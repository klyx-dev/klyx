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
class ValueTypeTest {

    private val context = ParseContext("ValueTypeTest.wasm", 1, 1)
    private val tokenizer = Tokenizer()

    @Test
    fun parseValidValueType() {
        var expected: ParseResult<ValueType> =
            ParseResult(
                ValueType.I32,
                1
            )
        var actual = tokenizer.tokenize("i32", context).parseValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(ValueType.I64, 1)
        actual = tokenizer.tokenize("i64", context).parseValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(ValueType.F32, 1)
        actual = tokenizer.tokenize("f32", context).parseValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(ValueType.F64, 1)
        actual = tokenizer.tokenize("f64", context).parseValueType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidValueType_ValidKeyword() {
        val exception =
            assertThrows(ParseException::class.java) { tokenizer.tokenize("abc", context).parseValueType(0) }
        assertThat(exception).hasMessageThat().contains("Invalid ValueType: Expecting i32, i64, f32, or f64")
    }

    @Test
    fun parseInvalidValueType_invalidKeyword() {
        val exception =
            assertThrows(ParseException::class.java) { tokenizer.tokenize("\$abc", context).parseValueType(0) }
        assertThat(exception).hasMessageThat().contains("Invalid ValueType: Expecting keyword token")
    }

    @Test
    fun parseOptional_withValidValueType() {
        var expected: ParseResult<ValueType> =
            ParseResult(
                ValueType.I32,
                1
            )
        var actual = tokenizer.tokenize("i32", context).parseOptionalValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(ValueType.I64, 1)
        actual = tokenizer.tokenize("i64", context).parseOptionalValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(ValueType.F32, 1)
        actual = tokenizer.tokenize("f32", context).parseOptionalValueType(0)
        assertThat(actual).isEqualTo(expected)

        expected = ParseResult(ValueType.F64, 1)
        actual = tokenizer.tokenize("f64", context).parseOptionalValueType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseOptional_withInvalidValueType_returnsNull() {
        val result = tokenizer.tokenize("abc").parseOptionalValueType(0)
        assertThat(result).isNull()
    }

    @Test
    fun parsePlural_throwsWhenNotEnough() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("i32").parseValueTypes(0, minRequired = 2)
        }
        assertThat(e).hasMessageThat().contains("Not enough ValueTypes")
    }

    @Test
    fun parsePlural_throwsWhenTooMany() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("i32 i64").parseValueTypes(0, maxAllowed = 1)
        }
        assertThat(e).hasMessageThat().contains("Too many ValueTypes")
    }

    @Test
    fun parsePlural_withDefaults_returnsEmptyList_forEmptyString() {
        val result = tokenizer.tokenize("").parseValueTypes(0)
        assertThat(result.parseLength).isEqualTo(0)
        assertThat(result.astNode).isEmpty()
    }

    @Test
    fun parsePlural() {
        val result = tokenizer.tokenize("i32 i64 f32 f64").parseValueTypes(0)
        assertThat(result.parseLength).isEqualTo(4)
        assertThat(result.astNode)
            .containsExactly(ValueType.I32, ValueType.I64, ValueType.F32, ValueType.F64)
            .inOrder()
    }
}
