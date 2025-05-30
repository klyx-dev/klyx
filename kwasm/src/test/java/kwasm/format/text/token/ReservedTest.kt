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
class ReservedTest {
    @Test
    fun findReserved_returnsValidMatch_whenExists() {
        val match = RawToken("  \t   ThisIsA_reser-ved! (;) \t  ", CONTEXT)
            .findReserved() ?: fail("Reserved not found")
        assertThat(match.sequence).isEqualTo("ThisIsA_reser-ved!")
        assertThat(match.index).isEqualTo(6)
    }

    @Test
    fun findReserved_returnsLongestMatch_whenMultipleExist() {
        val match = RawToken("short longer longest", CONTEXT)
            .findReserved() ?: fail("Reserved not found")
        assertThat(match.sequence).isEqualTo("longest")
        assertThat(match.index).isEqualTo(13)
    }

    @Test
    fun findReserved_returnsNull_whenNoneFound() {
        assertThat(RawToken("", CONTEXT).findReserved()).isNull()
        assertThat(RawToken("   (   )  ; ", CONTEXT).findReserved()).isNull()
    }

    @Test
    fun isReserved_returnsTrue_whenEntireSequenceIsReserved() {
        assertThat(RawToken("this-is-a-reserved-word", CONTEXT).isReserved()).isTrue()
    }

    @Test
    fun isReserved_returnsFalse_whenEntireSequenceIsNotReserved() {
        assertThat(RawToken("", CONTEXT).isReserved()).isFalse()
        assertThat(RawToken("    this-is-a-reserved-word", CONTEXT).isReserved()).isFalse()
        assertThat(RawToken("this-is-a-reserved-word    ", CONTEXT).isReserved()).isFalse()
    }

    companion object {
        private val CONTEXT = ParseContext("unknown", 1, 1)
    }
}
