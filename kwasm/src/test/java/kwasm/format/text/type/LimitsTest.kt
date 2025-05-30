/*
 * Copyright 2020 Google LLC
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

package kwasm.format.text.type

import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class LimitsTest {

    private val context = ParseContext("TokenizerTest.wasm", 1, 1)
    private val tokenizer = Tokenizer()

    @Test
    fun parseSingleNumber_setMinToCorrectValue() {
        val expectedMin = 123456
        val expectedMax = null
        val tokens = tokenizer.tokenize("$expectedMin", context)
        val parseResult = tokens.parseLimits(0)
        assertThat(parseResult.astNode.min).isEqualTo(expectedMin)
        assertThat(parseResult.astNode.max).isEqualTo(expectedMax)
        assertThat(parseResult.parseLength).isEqualTo(1)
    }

    @Test
    fun parseMaxVal_setMinToMaxVal() {
        val expectedMin = UInt.MAX_VALUE.toLong()
        val expectedMax = null
        val tokens = tokenizer.tokenize("$expectedMin", context)
        val parseResult = tokens.parseLimits(0)
        assertThat(parseResult.astNode.min).isEqualTo(expectedMin)
        assertThat(parseResult.astNode.max).isEqualTo(expectedMax)
        assertThat(parseResult.parseLength).isEqualTo(1)
    }

    @Test
    fun parseMinVal_setMinToMinVal() {
        val expectedMin = 0
        val expectedMax = null
        val tokens = tokenizer.tokenize("$expectedMin", context)
        val parseResult = tokens.parseLimits(0)
        assertThat(parseResult.astNode.min).isEqualTo(expectedMin)
        assertThat(parseResult.astNode.max).isEqualTo(expectedMax)
        assertThat(parseResult.parseLength).isEqualTo(1)
    }

    @Test
    fun parseNegativeMin_throwsParseExceptionWithNegativeNumberMessage() {
        val tokens = tokenizer.tokenize("-123456", context)
        assertThrows(ParseException::class.java) { tokens.parseLimits(0) }
    }

    @Test
    fun parseABitLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val tokens = tokenizer.tokenize("4294967296", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseLimits(0) }
        assertThat(exception).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun parseALotLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val tokens = tokenizer.tokenize("100000000000", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseLimits(0) }
        assertThat(exception).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun parseTwoNumbers_setMinAndMaxToCorrectValue() {
        val expectedMin = 123456
        val expectedMax = 234567
        val tokens = tokenizer.tokenize("$expectedMin $expectedMax", context)
        val parseResult = tokens.parseLimits(0)
        assertThat(parseResult.astNode.min).isEqualTo(expectedMin)
        assertThat(parseResult.astNode.max).isEqualTo(expectedMax)
        assertThat(parseResult.parseLength).isEqualTo(2)
    }

    @Test
    fun parseTwoValuesWithNegativeMin_throwsParseExceptionWithNegativeNumberMessage() {
        val tokens = tokenizer.tokenize("-123456 234567", context)
        assertThrows(ParseException::class.java) { tokens.parseLimits(0) }
    }

    @Test
    fun parseTwoValuesWithMinABitLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val tokens = tokenizer.tokenize("4294967296 234567", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseLimits(0) }
        assertThat(exception).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun parseTwoValuesWithMinALotLargerThanMaxVal_throwsParseExceptionWithValueOverflowMessage() {
        val tokens = tokenizer.tokenize("100000000000 234567", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseLimits(0) }
        assertThat(exception).hasMessageThat().contains("Illegal value")
    }

    @Test
    fun parseTwoValuesWithMaxALotLargerThanMaxVal_throws() {
        val tokens = tokenizer.tokenize("1234567 100000000000", context)
        assertThrows(ParseException::class.java) {
            tokens.parseLimits(0)
        }
    }

    @Test
    fun parseNoValues_throwsParseExceptionWithIncorrectNumberOfArgumentsException() {
        val tokens = tokenizer.tokenize("", context)
        val exception = assertThrows(ParseException::class.java) { tokens.parseLimits(0) }
        assertThat(exception).hasMessageThat().contains("Expected i32")
    }
}
