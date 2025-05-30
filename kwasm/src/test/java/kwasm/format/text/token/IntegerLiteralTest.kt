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
import org.assertj.core.api.Assertions
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.absoluteValue

@Suppress("EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class IntegerLiteralTest {
    @Test
    fun parseSigned_throwsWhenOutOfRange_i64Dec() {
        assertThrows(ParseException::class.java) {
            val literal = IntegerLiteral.Signed("18446744073709551616")
            literal.value
        }
    }

    @Test
    fun parseSigned_throwsWhenOutOfRange_i64Hex() {
        assertThrows(ParseException::class.java) {
            val literal = IntegerLiteral.Signed("-0x8000000000000001")
            literal.value
        }
    }

    @Test
    fun parsesUnsigned_base10() {
        val expected = 1234567890.toULong()
        val actual = IntegerLiteral.Unsigned("1234567890")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun throwsOnUnsigned_whenBase10_containsHexChars() {
        val literal = IntegerLiteral.Unsigned("1234aa")
        assertThrows(ParseException::class.java) { literal.value }
    }

    @Test
    fun parsesUnsigned_base16() {
        val expected = "1234567".toULong(16)
        val actual = IntegerLiteral.Unsigned("0x1234567")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun throwsOnUnsigned_outsideMagnitude() {
        val literal = IntegerLiteral.Unsigned("256", 8)
        val exception = assertThrows(ParseException::class.java) { literal.value }
        assertThat(exception).hasMessageThat().contains("Integer constant out of range")
    }

    @Test
    fun parsesUnsigned_withinMagnitude() {
        var actual = IntegerLiteral.Unsigned("255", 8)
        assertThat(actual.value).isEqualTo(255.toULong())

        actual = IntegerLiteral.Unsigned("3", 2)
        assertThat(actual.value).isEqualTo(3.toULong())
    }

    @Test
    fun parsesSigned_base10_noExplicitSign() {
        val expected = "123456789".toLong()
        val actual = IntegerLiteral.Signed("$expected")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parsesSigned_base10_explicitPositive() {
        val expected = "123456789".toLong()
        val actual = IntegerLiteral.Signed("+$expected")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parsesSigned_base10_explicitNegative() {
        val expected = "-123456789".toLong()
        val actual = IntegerLiteral.Signed("$expected")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun throwsOnSigned_whenBase10_containsHexChars() {
        val literal = IntegerLiteral.Signed("-1234aa")
        assertThrows(ParseException::class.java) { literal.value }
    }

    @Test
    fun parsesSigned_base16_noExplicitSign() {
        val expected = "1234567".toLong(16)
        val actual = IntegerLiteral.Signed("0x${expected.toString(16)}")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parsesSigned_base16_explicitPositive() {
        val expected = "1234567".toLong(16)
        val actual = IntegerLiteral.Signed("+0x${expected.toString(16)}")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parsesSigned_base16_explicitNegative() {
        val expected = -1 * "1234567".toLong(16)
        val actual = IntegerLiteral.Signed("-0x${expected.absoluteValue.toString(16)}")
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun parsesSigned_withinMagnitude() {
        var actual = IntegerLiteral.Signed("+100", 8)
        assertThat(actual.value).isEqualTo(100L)

        actual = IntegerLiteral.Signed("+127", 8)
        assertThat(actual.value).isEqualTo(127L)

        actual = IntegerLiteral.Signed("-128", 8)
        assertThat(actual.value).isEqualTo(-128L)
    }

    @Test
    fun throwsOnSigned_whenOutsideMagnitude() {
        val literal1 = IntegerLiteral.Signed("-9", 4)
        val exception1 = assertThrows(ParseException::class.java) { literal1.value }
        assertThat(exception1).hasMessageThat().contains("Illegal value")

        val literal2 = IntegerLiteral.Signed("8", 4)
        val exception2 = assertThrows(ParseException::class.java) { literal2.value }
        assertThat(exception2).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun findIntegerLiteral_whenExists_returnsValidTokenMatchResult() {
        val input = RawToken("     +0.5e10      asldkj", CONTEXT)
        val actual = input.findIntegerLiteral() ?: Assertions.fail("Didn't find int literal.")
        assertThat(actual.sequence).isEqualTo("+0")
        assertThat(actual.index).isEqualTo(5)
    }

    @Test
    fun findIntegerLiteral_whenMultipleExist_returnsLongestValidTokenMatchResult() {
        val input = RawToken("     +0.5e10   -0x1234.56789   asldkj", CONTEXT)
        val actual = input.findIntegerLiteral() ?: Assertions.fail("Didn't find int literal.")
        assertThat(actual.sequence).isEqualTo("-0x1234")
        assertThat(actual.index).isEqualTo(15)
    }

    @Test
    fun findIntegerLiteral_whenNoneExist_returnsNull() {
        val input = RawToken("non num", CONTEXT)
        assertThat(input.findIntegerLiteral()).isNull()
    }

    @Test
    fun isIntegerLiteral_returnsTrue_ifEntireSequenceIsFloat() {
        val input = RawToken("-12345789", CONTEXT)
        assertThat(input.isIntegerLiteral()).isTrue()
    }

    @Test
    fun isIntegerLiteral_returnsFalse_ifEntireSequenceIsNotAFloat() {
        assertThat(RawToken("non num", CONTEXT).isIntegerLiteral()).isFalse()
        assertThat(RawToken("1 num", CONTEXT).isIntegerLiteral()).isFalse()
        assertThat(RawToken("non 1", CONTEXT).isIntegerLiteral()).isFalse()
        assertThat(RawToken("-1.5e10", CONTEXT).isIntegerLiteral()).isFalse()
    }

    companion object {
        private val CONTEXT = ParseContext("unknown", 1, 1)
    }
}
