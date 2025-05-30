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
import kwasm.format.ParseContext
import kwasm.format.text.token.FloatLiteral
import kwasm.format.text.token.Identifier
import kwasm.format.text.token.IntegerLiteral
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Reserved
import kwasm.format.text.token.StringLiteral
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class TokenizerTest {
    private val context = ParseContext("TokenizerTest.wasm", 1, 1)
    private val tokenizer = Tokenizer()

    @Test
    fun tokenize_ofEmptyString_givesEmptyResults() {
        assertThat(tokenizer.tokenize("", context)).isEmpty()
    }

    @Test
    fun tokenize_ofStringLiteralWithNumber_givesResult() {
        val result = tokenizer.tokenize("\"test1\"", context)
        assertThat((result[0] as StringLiteral).value).isEqualTo("test1")
    }

    @Test
    fun tokenize_bunchOfKeywords_givesBunchOfKeywords() {
        val actual = tokenizer.tokenize(
            """
            here's-a-keyword
            
            (; block comment ;)
            
            and_another
            
            two more
            
            ;; comments shouldn't register
            
            """.trimIndent(),
            context
        )

        assertThat(actual).hasSize(4)

        val first = requireNotNull(actual[0] as? Keyword)
        assertThat(first.value).isEqualTo("here's-a-keyword")

        val second = requireNotNull(actual[1] as? Keyword)
        assertThat(second.value).isEqualTo("and_another")

        val third = requireNotNull(actual[2] as? Keyword)
        assertThat(third.value).isEqualTo("two")

        val fourth = requireNotNull(actual[3] as? Keyword)
        assertThat(fourth.value).isEqualTo("more")
    }

    @Test
    fun tokenize_bunchOfIntegers_givesBunchOfIntegerLiterals() {
        val actual = tokenizer.tokenize(
            """
            0
            0x54ab
            1_000_000
            -3
            """.trimIndent(),
            context
        )

        val first = requireNotNull(actual[0] as? IntegerLiteral.Unsigned)
        assertThat(first.value).isEqualTo(0uL)

        val second = requireNotNull(actual[1] as? IntegerLiteral.Unsigned)
        assertThat(second.value).isEqualTo(0x54abuL)

        val third = requireNotNull(actual[2] as? IntegerLiteral.Unsigned)
        assertThat(third.value).isEqualTo(1000000uL)

        val fourth = requireNotNull(actual[3] as? IntegerLiteral.Signed)
        assertThat(fourth.value).isEqualTo(-3L)
    }

    @Test
    fun tokenize_bunchOfFloats_givesBunchOfFloatLiterals() {
        val actual = tokenizer.tokenize(
            """
            (;
                Hidden float, in a comment: 0.0
            ;)
            1.5e10
            
            ;; another float in a comment: 10.1
            
            -1.0 0.0000001
            +0x54aP-15
            """.trimIndent(),
            context
        )

        assertThat(actual).hasSize(4)

        val first = requireNotNull(actual[0] as? FloatLiteral)
        assertThat(first.value.toDouble()).isWithin(TOLERANCE).of(1.5e10)

        val second = requireNotNull(actual[1] as? FloatLiteral)
        assertThat(second.value.toDouble()).isWithin(TOLERANCE).of(-1.0)

        val third = requireNotNull(actual[2] as? FloatLiteral)
        assertThat(third.value.toDouble()).isWithin(TOLERANCE).of(0.0000001)

        val fourth = requireNotNull(actual[3] as? FloatLiteral)
        assertThat(fourth.value.toDouble()).isWithin(TOLERANCE)
            .of(FloatLiteral("+0x54aP-15").value.toDouble())
    }

    @Test
    fun tokenize_bunchOfStrings_givesBunchOfStringLiterals() {
        val actual = tokenizer.tokenize(
            """
            "this is a string"
            
            "so is this" "and this"
            
            ""
            """.trimIndent(),
            context
        )

        assertThat(actual).hasSize(4)

        val first = requireNotNull(actual[0] as? StringLiteral)
        assertThat(first.value).isEqualTo("this is a string")

        val second = requireNotNull(actual[1] as? StringLiteral)
        assertThat(second.value).isEqualTo("so is this")

        val third = requireNotNull(actual[2] as? StringLiteral)
        assertThat(third.value).isEqualTo("and this")

        val fourth = requireNotNull(actual[3] as? StringLiteral)
        assertThat(fourth.value).isEmpty()
    }

    @Test
    fun tokenize_bunchOfIdentifiers_givesBunchOfIdentifiers() {
        val actual = tokenizer.tokenize(
            """
            ${'$'}here
            
               ${'$'}are ${'$'}some_identifiers 
            ;; word
            ${'$'}${'$'}forya
            """.trimIndent(),
            context
        )

        assertThat(actual).hasSize(4)

        val first = requireNotNull(actual[0] as? Identifier)
        assertThat(first.value).isEqualTo("\$here")

        val second = requireNotNull(actual[1] as? Identifier)
        assertThat(second.value).isEqualTo("\$are")

        val third = requireNotNull(actual[2] as? Identifier)
        assertThat(third.value).isEqualTo("\$some_identifiers")

        val fourth = requireNotNull(actual[3] as? Identifier)
        assertThat(fourth.value).isEqualTo("\$\$forya")
    }

    @Test
    fun tokenize_bunchOfParens_givesBunchOfParens() {
        val actual = tokenizer.tokenize(
            """
            ((((
            ((((
            ))))
            ))))
            """.trimIndent(),
            context
        )
        assertThat(actual).hasSize(16)
        actual.forEachIndexed { index, token ->
            if (index < 8) {
                assertThat(token).isInstanceOf(Paren.Open::class.java)
            } else {
                assertThat(token).isInstanceOf(Paren.Closed::class.java)
            }
        }
    }

    @Test
    fun tokenize_bunchOfReserved_givesBunchOfReserved() {
        val actual = tokenizer.tokenize(
            """
            HereIs_aReserved
            
            AlsoHereToo  AmIReserved?
            
            Yes
            """.trimIndent(),
            context
        )

        assertThat(actual).hasSize(4)

        val first = requireNotNull(actual[0] as? Reserved)
        assertThat(first.value).isEqualTo("HereIs_aReserved")

        val second = requireNotNull(actual[1] as? Reserved)
        assertThat(second.value).isEqualTo("AlsoHereToo")

        val third = requireNotNull(actual[2] as? Reserved)
        assertThat(third.value).isEqualTo("AmIReserved?")

        val fourth = requireNotNull(actual[3] as? Reserved)
        assertThat(fourth.value).isEqualTo("Yes")
    }

    companion object {
        private const val TOLERANCE = 0.0000001
    }
}
