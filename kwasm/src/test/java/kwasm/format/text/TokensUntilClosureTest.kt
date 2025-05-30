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
import kwasm.format.ParseException
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TokensUntilClosureTest {
    private val tokenizer = Tokenizer()

    @Test
    fun returnsCorrectEnclosedTokens() {
        val tokens = tokenizer.tokenize("(foo (bar) (baz))", null)
        val forFoo = tokens.tokensUntilParenClosure(1, 1)
        assertThat(forFoo).containsExactly(
            Keyword("foo"),
            Paren.Open(),
            Keyword("bar"),
            Paren.Closed(),
            Paren.Open(),
            Keyword("baz"),
            Paren.Closed(),
            Paren.Closed()
        ).inOrder()
    }

    @Test
    fun returnsCorrectEnclosedTokens_extraClosure() {
        val tokens = tokenizer.tokenize("(foo (bar) (baz))", null)
        val forFoo = tokens.tokensUntilParenClosure(3, 2)
        assertThat(forFoo).containsExactly(
            Keyword("bar"),
            Paren.Closed(),
            Paren.Open(),
            Keyword("baz"),
            Paren.Closed(),
            Paren.Closed()
        ).inOrder()
    }

    @Test
    fun throwsIf_closuresCantBeFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(foo (bar) (baz))")
                .tokensUntilParenClosure(1, 2)
        }
    }
}
