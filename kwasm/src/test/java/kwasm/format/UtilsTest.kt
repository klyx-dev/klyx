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

package kwasm.format

import com.google.common.truth.Truth.assertThat
import kwasm.format.text.Tokenizer
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isOpenParen
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UtilsTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("UtilsTest.wat")

    @Test
    fun isOpenParen() {
        val tokens = tokenizer.tokenize("(asdf \"\" 1234)", context)
        assertThat(tokens.isOpenParen(0)).isTrue()
        (1 until tokens.size).forEach {
            assertThat(tokens.isOpenParen(it)).isFalse()
        }
    }

    @Test
    fun isClosedParen() {
        val tokens = tokenizer.tokenize("(asdf \"\" 1234)", context)
        (0 until (tokens.size - 1)).forEach {
            assertThat(tokens.isClosedParen(it)).isFalse()
        }
        assertThat(tokens.isClosedParen(tokens.size - 1)).isTrue()
    }

    @Test
    fun contextAt() {
        val context = tokenizer.tokenize("\n\n\n  $0", context).contextAt(0)
            ?: fail("Shouldn't be null")
        assertThat(context.column).isEqualTo(3)
        assertThat(context.lineNumber).isEqualTo(4)

        assertThat(tokenizer.tokenize("", context).contextAt(0)).isNull()
        assertThat(tokenizer.tokenize("", context).contextAt(1)).isNull()
        assertThat(tokenizer.tokenize("", context).contextAt(10)).isNull()
        assertThat(tokenizer.tokenize("(", context).contextAt(1)).isNotNull()
        assertThat(tokenizer.tokenize("(", context).contextAt(10)).isNotNull()
    }
}
