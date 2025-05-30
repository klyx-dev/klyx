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
import kwasm.format.ParseException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class NumTest {
    @Test
    fun parsesSingleDigits_base10() {
        repeat(10) { digit ->
            val num = Num("$digit")
            assertThat(num.value).isEqualTo(digit.toULong())
            assertThat(num.foundHexChars).isFalse()
        }
    }

    @Test
    fun parsesSingleDigits_base16_lowercase() {
        val chars = "abcdef"
        val values = listOf(10, 11, 12, 13, 14, 15).map { it.toULong() }
        chars.forEachIndexed { index, value ->
            val num = Num("$value")
            assertThat(num.value).isEqualTo(values[index])
            assertThat(num.foundHexChars).isTrue()
        }
    }

    @Test
    fun parsesSingleDigits_base16_uppercase() {
        val chars = "ABCDEF"
        val values = listOf(10, 11, 12, 13, 14, 15).map { it.toULong() }
        chars.forEachIndexed { index, value ->
            val num = Num("$value")
            assertThat(num.value).isEqualTo(values[index])
            assertThat(num.foundHexChars).isTrue()
        }
    }

    @Test
    fun parsesBase10() {
        val expected = 1234567890.toULong()
        val num = Num("$expected")
        assertThat(num.value).isEqualTo(expected)
    }

    @Test
    fun parsesBase10_withUnderscores() {
        val expected = 1234567890.toULong()
        var num = Num("${expected}_")
        assertThat(num.value).isEqualTo(expected)

        num = Num("_$expected")
        assertThat(num.value).isEqualTo(expected)

        num = Num("_1_2_3_4_5_6_7_8_9_0_")
        assertThat(num.value).isEqualTo(expected)
    }

    @Test
    fun parsesBase16() {
        val sequence = "1a2b3c4d"
        val expected = sequence.toULong(16)
        val num = Num(sequence)
        assertThat(num.value).isEqualTo(expected)
    }

    @Test
    fun parsesBase16_withUnderScores() {
        val sequence = "1a2b3c4d"
        val expected = sequence.toULong(16)

        var num = Num(sequence)
        assertThat(num.value).isEqualTo(expected)

        num = Num("${sequence}_")
        assertThat(num.value).isEqualTo(expected)

        num = Num("_$sequence")
        assertThat(num.value).isEqualTo(expected)

        num = Num("_1_a_2_b_3_c_4_d_")
        assertThat(num.value).isEqualTo(expected)
    }

    @Test
    fun parsesBase16_whenForcingHex() {
        val sequence = "123456789"
        val expected = sequence.toULong(16)
        val num = Num(sequence)
        assertThat(num.foundHexChars).isFalse()
        num.forceHex = true
        assertThat(num.value).isEqualTo(expected)
    }

    @Test
    fun throwsOnEmptySequence() {
        val num = Num("")
        assertThrows(ParseException::class.java) { num.value }
    }

    @Test
    fun throwsOnIllegalChar() {
        val sequence = "10z00"
        val num = Num(sequence)
        assertThrows(ParseException::class.java) { num.value }
    }
}
