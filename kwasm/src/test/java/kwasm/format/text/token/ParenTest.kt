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
class ParenTest {
    @Test
    fun findParen_returnsValidMatch_whenExists() {
        var match = RawToken("   ( other stuff  ", CONTEXT)
            .findParen() ?: fail("No paren found")
        assertThat(match.sequence).isEqualTo("(")
        assertThat(match.index).isEqualTo(3)

        match = RawToken("   ) other stuff  ", CONTEXT)
            .findParen() ?: fail("No paren found")
        assertThat(match.sequence).isEqualTo(")")
        assertThat(match.index).isEqualTo(3)
    }

    @Test
    fun findParen_returnsNull_whenNoParenExists() {
        assertThat(RawToken("", CONTEXT).findParen()).isNull()
        assertThat(RawToken("no parens here!", CONTEXT).findParen()).isNull()
        assertThat(RawToken("  -1.5e10", CONTEXT).findParen()).isNull()
    }

    @Test
    fun isOpenParen_returnsTrue_ifEntireSequence_isOpenParen() {
        assertThat(RawToken("(", CONTEXT).isOpenParen()).isTrue()
    }

    @Test
    fun isOpenParen_returnsFalse_ifEntireSequence_isNotOpenParen() {
        assertThat(RawToken(" (", CONTEXT).isOpenParen()).isFalse()
        assertThat(RawToken("( ", CONTEXT).isOpenParen()).isFalse()
        assertThat(RawToken(")", CONTEXT).isOpenParen()).isFalse()
    }

    @Test
    fun isClosedParen_returnsTrue_ifEntireSequence_isClosedParen() {
        assertThat(RawToken(")", CONTEXT).isClosedParen()).isTrue()
    }

    @Test
    fun isClosedParen_returnsFalse_ifEntireSequence_isNotClosedParen() {
        assertThat(RawToken(" )", CONTEXT).isClosedParen()).isFalse()
        assertThat(RawToken(") ", CONTEXT).isClosedParen()).isFalse()
        assertThat(RawToken("(", CONTEXT).isClosedParen()).isFalse()
    }

    companion object {
        private val CONTEXT = ParseContext("unknown", 1, 1)
    }
}
