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

package kwasm.format.text.token.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UtilsTest {
    @Test
    fun parseStringChar_parsesPlainChars() {
        val sequence =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'_+-*{}/!@#$%^&*()=[]?" +
                "<>,.`~|"
        for (i in sequence.indices) {
            assertThat(sequence.parseStringChar(i).toString()).isEqualTo("${sequence[i]}")
        }
    }

    @Test
    fun parseStringChar_parsesEscapedTab() {
        val sequence = "\\t"
        val actual = sequence.parseStringChar(0)
        assertThat(actual.toString()).isEqualTo("\t")
        assertThat(actual.sequenceLength).isEqualTo(2)
    }

    @Test
    fun parseStringChar_parsesEscapedNewline() {
        val sequence = "\\n"
        val actual = sequence.parseStringChar(0)
        assertThat(actual.toString()).isEqualTo("\n")
        assertThat(actual.sequenceLength).isEqualTo(2)
    }

    @Test
    fun parseStringChar_parsesEscapedReturn() {
        val sequence = "\\r"
        val actual = sequence.parseStringChar(0)
        assertThat(actual.toString()).isEqualTo("\r")
        assertThat(actual.sequenceLength).isEqualTo(2)
    }

    @Test
    fun parseStringChar_parsesEscapedSingleQuote() {
        val sequence = "\\'"
        val actual = sequence.parseStringChar(0)
        assertThat(actual.toString()).isEqualTo("'")
        assertThat(actual.sequenceLength).isEqualTo(2)
    }

    @Test
    fun parseStringChar_parsesEscapedDoubleQuote() {
        val sequence = "\\\""
        val actual = sequence.parseStringChar(0)
        assertThat(actual.toString()).isEqualTo("\"")
        assertThat(actual.sequenceLength).isEqualTo(2)
    }

    @Test
    fun parseStringChar_parsesEscapedBackslash() {
        val sequence = "\\\\"
        val actual = sequence.parseStringChar(0)
        assertThat(actual.toString()).isEqualTo("\\")
        assertThat(actual.sequenceLength).isEqualTo(2)
    }

    @Test
    fun parseStringChar_parsesUnicode() {
        val sequence = "\\u{03C0}"
        val actual = sequence.parseStringChar(0)
        assertThat(actual.toString()).isEqualTo("π")
        assertThat(actual.sequenceLength).isEqualTo(8)
    }

    @Test
    fun parseStringChar_parsesUnicode_privateUseArea() {
        val sequence = "\\u{1F602}"
        val actual = sequence.parseStringChar(0)
        assertThat(actual.toString()).isEqualTo("😂")
        assertThat(actual.sequenceLength).isEqualTo(9)
    }

    @Test
    fun parseStringElem_parsesPairedHexDigits() {
        val sequence = "\\7F"
        val actual = sequence.parseStringElem(0)
        assertThat(actual.toString()).isEqualTo("\u007F")
        assertThat(actual.sequenceLength).isEqualTo(3)
    }

    @Test
    fun parseStringElem_parsesPlainChars() {
        val sequence =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'_+-*{}/!@#$%^&*()=[]?" +
                "<>,.`~|"
        for (i in sequence.indices) {
            assertThat(sequence.parseStringElem(i).toString()).isEqualTo("${sequence[i]}")
        }
    }

    @Test
    fun parseStringElem_parsesUnicode() {
        val sequence = "\\u{03C0}"
        val actual = sequence.parseStringElem(0)
        assertThat(actual.toString()).isEqualTo("π")
        assertThat(actual.sequenceLength).isEqualTo(8)
    }
}
