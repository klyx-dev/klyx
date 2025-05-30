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

package kwasm.format.text.token

import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseContext
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StringLiteralTest {
    @Test
    fun parsesEmptyString() {
        val actual = StringLiteral("\"\"")
        assertThat(actual.value).isEmpty()
    }

    @Test
    fun parsesStringWithNumber() {
        val actual = StringLiteral("\"test1\"")
        assertThat(actual.value).isEqualTo("test1")
    }

    @Test
    fun parsesSimpleString() {
        val actual =
            StringLiteral("\"This is a test of it's capabilities!\"")
        assertThat(actual.value).isEqualTo("This is a test of it's capabilities!")
    }

    @Test
    fun parsesComplexString() {
        val actual = StringLiteral(
            "\"Hello! \\u{1f44b}\\n\\u{4f60}\\u{597D} \\u{1F44B}\\n\\n\\\"wasm rocks\\\"\"",
            ParseContext("StringLiteralTest.kt", 0, 0)
        )
        assertThat(actual.value).isEqualTo("Hello! 👋\n你好 👋\n\n\"wasm rocks\"")
    }

    @Test
    fun parsesComplexString_withEmbeddedUnicode() {
        val actual =
            StringLiteral("\"Hello! 👋\\n你好 👋\\n\\n\\\"wasm rocks\\\"\"")
        assertThat(actual.value).isEqualTo("Hello! 👋\n你好 👋\n\n\"wasm rocks\"")
    }

    @Test
    fun findStringLiteral_returnsValidMatch_whenExists() {
        val text = "\"Hello! 👋\\n你好 👋\\n\\n\\\"wasm rocks\\\"\""
        val token = RawToken("not  a string $text  -0x547 \\\\u{1}", CONTEXT)
        val match = token.findStringLiteral() ?: fail("No string found")
        assertThat(match.sequence).isEqualTo(text)
        assertThat(match.index).isEqualTo(14)

        val emptyToken = RawToken("   \"\"", CONTEXT)
        val emptyMatch = emptyToken.findStringLiteral() ?: fail("No string found")
        assertThat(emptyMatch.sequence).isEqualTo("\"\"")
        assertThat(emptyMatch.index).isEqualTo(3)
    }

    @Test
    fun findStringLiteral_returnsLongestMatch_whenMultipleExist() {
        val token = RawToken("\"short\"   \"longer\"  \"longest\"", CONTEXT)
        val match = token.findStringLiteral() ?: fail("No string found")
        assertThat(match.sequence).isEqualTo("\"longest\"")
        assertThat(match.index).isEqualTo(20)
    }

    @Test
    fun findStringLiteral_returnsNull_whenNoneExist() {
        assertThat(RawToken("\" asdfasdf", CONTEXT).findStringLiteral()).isNull()
        assertThat(RawToken("asdfasdf \"", CONTEXT).findStringLiteral()).isNull()
        assertThat(RawToken("asdfasdf \"", CONTEXT).findStringLiteral()).isNull()
        assertThat(RawToken("", CONTEXT).findStringLiteral()).isNull()
        assertThat(RawToken("-1.5e10", CONTEXT).findStringLiteral()).isNull()
    }

    @Test
    fun isStringLiteral_returnsTrue_whenEntireSequenceIsString() {
        assertThat(RawToken("\"\"", CONTEXT).isStringLiteral()).isTrue()
        assertThat(RawToken("\"\\\"\"", CONTEXT).isStringLiteral()).isTrue()
        assertThat(
            RawToken(
                "\"Hello! 👋\\n你好 👋\\n\\n\\\"wasm rocks\\\"\"",
                CONTEXT
            ).isStringLiteral()
        ).isTrue()
    }

    @Test
    fun isStringLiteral_returnsFalse_whenSequence_isNotEntirely_String() {
        assertThat(RawToken("", CONTEXT).isStringLiteral()).isFalse()
        assertThat(
            RawToken("\"this is a string\" but_this_isn't", CONTEXT).isStringLiteral()
        ).isFalse()
        assertThat(
            RawToken("this isn't a string \"but_this_is\"", CONTEXT).isStringLiteral()
        ).isFalse()
    }

    companion object {
        private val CONTEXT = ParseContext("Unknown", 1, 1)
    }
}
