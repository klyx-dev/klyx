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
class KeywordTest {
    @Test
    fun findKeyword_findsValidMatch_whenExists() {
        val token = RawToken("323   aValidKeyword -1.5e10", CONTEXT)
        val match = token.findKeyword() ?: fail("Keyword not found")
        assertThat(match.sequence).isEqualTo("aValidKeyword")
        assertThat(match.index).isEqualTo(6)
    }

    @Test
    fun findKeyword_findsLongest_whenMultipleExist() {
        val token = RawToken("short longer longest", CONTEXT)
        val match = token.findKeyword() ?: fail("Keyword not found")
        assertThat(match.sequence).isEqualTo("longest")
        assertThat(match.index).isEqualTo(13)
    }

    @Test
    fun findKeyword_returnsNull_whenNoneExist() {
        assertThat(RawToken("", CONTEXT).findKeyword()).isNull()
        assertThat(RawToken("NOT_A_KEYWORD", CONTEXT).findKeyword()).isNull()
        assertThat(RawToken("-12345.6789", CONTEXT).findKeyword()).isNull()
    }
    @Test
    fun isKeyword_returnsTrue_ifEntireSequence_isAKeyword() {
        assertThat(RawToken("validKeyword", CONTEXT).isKeyword()).isTrue()
        assertThat(RawToken("also_valid", CONTEXT).isKeyword()).isTrue()
        assertThat(RawToken("also-valid", CONTEXT).isKeyword()).isTrue()
    }

    @Test
    fun isKeyword_returnsFalse_ifEntireSequence_isNotAKeyword() {
        assertThat(RawToken("NotAKeyword", CONTEXT).isKeyword()).isFalse()
        assertThat(RawToken("   keyword", CONTEXT).isKeyword()).isFalse()
        assertThat(RawToken("keyword   ", CONTEXT).isKeyword()).isFalse()
    }

    companion object {
        private val CONTEXT = ParseContext("unknown", 1, 1)
    }
}
