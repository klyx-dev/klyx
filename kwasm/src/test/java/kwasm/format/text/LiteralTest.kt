/*
 * Copyright 2019 Google LLC
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

package kwasm.format.text

import com.google.common.truth.Truth.assertThat
import kwasm.ast.IntegerLiteral
import kwasm.format.ParseException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class LiteralTest {
    private val tokenizer = Tokenizer()

    @Test
    fun parsesLiteral_unsignedInt() {
        val result = tokenizer.tokenize("0xFFFFFFFF").parseLiteral(0, UInt::class)
        assertThat(result.astNode).isInstanceOf(IntegerLiteral.U32::class.java)
        assertThat(result.astNode.value).isEqualTo(0xFFFFFFFFu)
    }

    @Test
    fun parsesLiteral_signedInt() {
        val result = tokenizer.tokenize("1337").parseLiteral(0, Int::class)
        assertThat(result.astNode.value).isEqualTo(1337)
    }

    @Test
    fun parsesLiteral_unsignedLong() {
        val result = tokenizer.tokenize("0xFFFFFFFFFFFFFFFF").parseLiteral(0, ULong::class)
        assertThat(result.astNode.value).isEqualTo(0xFFFFFFFFFFFFFFFFu)
    }

    @Test
    fun parsesLiteral_signedLong() {
        val result = tokenizer.tokenize("0xFFFFFFFFFFFFFFFF").parseLiteral(0, Long::class)
        assertThat(result.astNode.value).isEqualTo(-1)
    }

    @Test
    fun parsesLiteral_float() {
        val result = tokenizer.tokenize("0.5").parseLiteral(0, Float::class)
        assertThat(result.astNode.value).isEqualTo(0.5f)
    }

    @Test
    fun parsesLiteral_double() {
        val result = tokenizer.tokenize("0.5").parseLiteral(0, Double::class)
        assertThat(result.astNode.value).isEqualTo(0.5)
    }

    @Test
    fun parsesLiteral_string() {
        val result = tokenizer.tokenize("\"hello world\"").parseLiteral(0, String::class)
        assertThat(result.astNode.value).isEqualTo("hello world")
    }

    @Test
    fun throws_ifLiteral_isNotUInt_whenDesired() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("0x5p1").parseLiteral(0, UInt::class)
        }
        assertThat(exception).hasMessageThat().contains("Expected i32")
    }

    @Test
    fun throws_ifLiteral_isNotInt_whenDesired() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("0x5p1").parseLiteral(0, Int::class)
        }
        assertThat(exception).hasMessageThat().contains("Expected i32")
    }

    @Test
    fun throws_ifLiteral_isNotULong_whenDesired() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("0x5p1").parseLiteral(0, ULong::class)
        }
        assertThat(exception).hasMessageThat().contains("Expected i64")
    }

    @Test
    fun throws_ifLiteral_isNotLong_whenDesired() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("0x5p1").parseLiteral(0, Long::class)
        }
        assertThat(exception).hasMessageThat().contains("Expected i64")
    }

    @Test
    fun ifLiteral_isNotFloat_butIsInt() {
        val literal = tokenizer.tokenize("1234").parseLiteral(0, Float::class)
        assertThat(literal.astNode.value).isEqualTo(1234.0f)
    }

    @Test
    fun throws_ifLiteral_isNotFloat_isString() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("\"1234\"").parseLiteral(0, Float::class)
        }
        assertThat(exception).hasMessageThat().contains("Expected f32")
    }

    @Test
    fun ifLiteral_isNotDouble_butIsInt() {
        val literal = tokenizer.tokenize("1234").parseLiteral(0, Double::class)
        assertThat(literal.astNode.value).isEqualTo(1234.0)
    }

    @Test
    fun throws_ifLiteral_isNotDouble_isString() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("\"1234\"").parseLiteral(0, Double::class)
        }
        assertThat(exception).hasMessageThat().contains("Expected f64")
    }

    @Test
    fun throws_ifLiteral_isNotString_whenDesired() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("1234").parseLiteral(0, String::class)
        }
        assertThat(exception).hasMessageThat().contains("Expected String")
    }

    @Test
    fun throws_ifDesiredLiteralClass_notSupported() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("1234").parseLiteral(0, Char::class)
        }
        assertThat(exception).hasMessageThat()
            .contains("Type char is not a supported literal type")
    }
}
