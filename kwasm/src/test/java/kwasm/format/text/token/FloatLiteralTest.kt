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
import kwasm.format.ParseException
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.pow

@RunWith(JUnit4::class)
class FloatLiteralTest {
    @Test
    fun throws_when32Bit_andExponentValueIsTooLarge_hex() {
        val literal = FloatLiteral("0x1p128", 32)
        assertThrows(ParseException::class.java) {
            literal.value
        }
    }

    @Test
    fun parsesInf() {
        val actual = FloatLiteral("inf")
        assertThat(actual.isInfinite()).isTrue()
    }

    @Test
    fun parsesInf_32() {
        val actual = FloatLiteral("inf", 32)
        assertThat(actual.isInfinite()).isTrue()
    }

    @Test
    fun parsesNegInf() {
        val actual = FloatLiteral("-inf")
        assertThat(actual.isInfinite()).isTrue()
    }

    @Test
    fun parsesNegInf_32() {
        val actual = FloatLiteral("-inf", 32)
        assertThat(actual.isInfinite()).isTrue()
    }

    @Test
    fun parsesNaN() {
        val actual = FloatLiteral("nan")
        assertThat(actual.isNaN()).isTrue()
    }

    @Test
    fun parsesHexNaN() {
        val actual = FloatLiteral("nan:0xF")
        assertThat(actual.isNaN()).isTrue()
    }

    @Test
    fun parsesNaN_32() {
        val actual = FloatLiteral("nan", 32)
        assertThat(actual.isNaN()).isTrue()
    }

    @Test
    fun parsesHexNaN_32() {
        val actual = FloatLiteral("nan:0xF", 32)
        assertThat(actual.isNaN()).isTrue()
    }

    @Test
    fun parsesDecimalInts() {
        var actual = FloatLiteral("10")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(10.0)

        actual = FloatLiteral("0")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(0.0)

        actual = FloatLiteral("1000000000")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(1000000000.0)
    }

    @Test
    fun parsesHexInts() {
        var actual = FloatLiteral("0x10")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(16.0)

        actual = FloatLiteral("0x0")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(0.0)

        actual = FloatLiteral("0xFFFFFFFF")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(0xFFFFFFFFL.toDouble())
    }

    @Test
    fun parsesDecimal_withFraction() {
        var actual = FloatLiteral("1.")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(1.0)

        actual = FloatLiteral("1.5")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(1.5)

        actual = FloatLiteral("0.123456789")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(0.123456789)

        actual = FloatLiteral("123456789.123456789")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(123456789.123456789)
    }

    @Test
    fun parsesHex_withFraction() {
        var actual = FloatLiteral("0xF.")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(15.0)

        actual = FloatLiteral("0x10.a")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(16.0 + 10 / 16.0)
    }

    @Test
    fun parsesDecimal_withExponent() {
        var actual = FloatLiteral("1e10")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(1e10)

        actual = FloatLiteral("3e-5")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(3e-5)
    }

    @Test
    fun parsesHex_withExponent() {
        var actual = FloatLiteral("0xFp2")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(15 * 2.0.pow(2))

        actual = FloatLiteral("0xABp-2")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(0xAB * 2.0.pow(-2))
    }

    @Test
    fun parsesDecimal_withFraction_andExponent() {
        var actual = FloatLiteral("1.e10")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(1e10)

        actual = FloatLiteral("1.5e10")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(1.5e10)

        actual = FloatLiteral("3.12345e-4")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(3.12345e-4)
    }

    @Test
    fun parsesHex_withFraction_andExponent() {
        var actual = FloatLiteral("0xF.p2")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE).of(15 * 2.0.pow(2))

        actual = FloatLiteral("0xF.ap2")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE)
            .of((15 + 10 / 16.0) * 2.0.pow(2))

        actual = FloatLiteral("0xAB.1p-2")
        assertThat(actual.value.toDouble()).isWithin(TOLERANCE)
            .of((0xAB + 1 / 16.0) * 2.0.pow(-2))
    }

    @Test
    fun decimalThrows_whenHexEncountered() {
        // Case 1.
        var actual = FloatLiteral("a")
        assertThrows(ParseException::class.java) { actual.value }

        // Case 2.
        actual = FloatLiteral("a.1")
        assertThrows(ParseException::class.java) { actual.value }

        // Case 2.
        actual = FloatLiteral("1.a")
        assertThrows(ParseException::class.java) { actual.value }

        // Case 3.
        actual = FloatLiteral("aE1")
        assertThrows(ParseException::class.java) { actual.value }

        // Case 3.
        actual = FloatLiteral("1ea")
        assertThrows(ParseException::class.java) { actual.value }

        // Case 4.
        actual = FloatLiteral("5.1eA")
        assertThrows(ParseException::class.java) { actual.value }

        // Case 4.
        actual = FloatLiteral("5.Ae1")
        assertThrows(ParseException::class.java) { actual.value }

        // Case 4.
        actual = FloatLiteral("A.5e1")
        assertThrows(ParseException::class.java) { actual.value }
    }

    @Test
    fun decimalThrows_whenDotIsFirstElement() {
        val actual = FloatLiteral(".5")
        val exception = assertThrows(ParseException::class.java) { actual.value }
        assertThat(exception).hasMessageThat().contains("Invalid placement for decimal")
    }

    @Test
    fun decimalThrows_whenExponentIsEmpty() {
        val actual = FloatLiteral("0.5e")
        val exception = assertThrows(ParseException::class.java) { actual.value }
        assertThat(exception).hasMessageThat().contains("Invalid exponent")
    }

    @Test
    fun hexThrows_whenExponentIsEmpty() {
        val actual = FloatLiteral("0x0.Ap")
        val exception = assertThrows(ParseException::class.java) { actual.value }
        assertThat(exception).hasMessageThat().contains("Invalid exponent")
    }

    @Test
    fun findFloatLiteral_whenExists_returnsValidTokenMatchResult() {
        val input = RawToken("     +0.5e10      asldkj", CONTEXT)
        val actual = input.findFloatLiteral() ?: fail("Didn't find float literal.")
        assertThat(actual.sequence).isEqualTo("+0.5e10")
        assertThat(actual.index).isEqualTo(5)
    }

    @Test
    fun findFloatLiteral_whenMultipleExist_returnsLongestValidTokenMatchResult() {
        val input = RawToken("     +0.5e10   -0x1234.56789   asldkj", CONTEXT)
        val actual = input.findFloatLiteral() ?: fail("Didn't find float literal.")
        assertThat(actual.sequence).isEqualTo("-0x1234.56789")
        assertThat(actual.index).isEqualTo(15)
    }

    @Test
    fun findFloatLiteral_whenNoneExist_returnsNull() {
        val input = RawToken("non num", CONTEXT)
        assertThat(input.findFloatLiteral()).isNull()
    }

    @Test
    fun isFloatLiteral_returnsTrue_ifEntireSequenceIsFloat() {
        val input = RawToken("-12345789.01e+34", CONTEXT)
        assertThat(input.isFloatLiteral()).isTrue()
    }

    @Test
    fun isFloatLiteral_returnsTrue_ifEntireSequenceIsFloat_nan() {
        val input = RawToken("nan", CONTEXT)
        assertThat(input.isFloatLiteral()).isTrue()
    }

    @Test
    fun isFloatLiteral_returnsTrue_ifEntireSequenceIsFloat_inf() {
        val input = RawToken("inf", CONTEXT)
        assertThat(input.isFloatLiteral()).isTrue()
    }

    @Test
    fun isFloatLiteral_returnsFalse_ifEntireSequenceIsNotAFloat() {
        assertThat(RawToken("non num", CONTEXT).isFloatLiteral()).isFalse()
        assertThat(RawToken("1 num", CONTEXT).isFloatLiteral()).isFalse()
        assertThat(RawToken("non 1", CONTEXT).isFloatLiteral()).isFalse()
    }

    companion object {
        private const val TOLERANCE = 1e-10
        private val CONTEXT = ParseContext("Unknown.wast", 1, 1)
    }
}
