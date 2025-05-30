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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FracTest {
    @Test
    fun empty_base10_parsesToZero() {
        val actual = Frac("")
        assertThat(actual.value).isEqualTo(0.0)
    }

    @Test
    fun empty_base16_parsesToZero() {
        val actual = Frac("")
        actual.forceHex = true
        assertThat(actual.value).isEqualTo(0.0)
    }

    @Test
    fun singleDigit_base10_returnsTenths() {
        var actual = Frac("1")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.1)

        actual = Frac("0")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.0)

        actual = Frac("2")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.2)

        actual = Frac("5")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.5)

        actual = Frac("9")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.9)
    }

    @Test
    fun singleDigit_base16_returnsSixteenths() {
        var actual = Frac("1")
        actual.forceHex = true
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(1.0 / 16.0)

        actual = Frac("0")
        actual.forceHex = true
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.0)

        actual = Frac("2")
        actual.forceHex = true
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(2.0 / 16.0)

        actual = Frac("8")
        actual.forceHex = true
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.5)

        actual = Frac("a")
        actual.forceHex = true
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(10.0 / 16.0)

        actual = Frac("f")
        actual.forceHex = true
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(15.0 / 16.0)
    }

    @Test
    fun noUnderscores_base10() {
        var actual = Frac("00")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.0)

        actual = Frac("10")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.1)

        actual = Frac("01")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.01)

        actual = Frac("11")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.11)

        actual = Frac("55")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.55)

        actual = Frac("99")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.99)

        actual = Frac("999999")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.999999)
    }

    @Test
    fun underscores_base10() {
        var actual = Frac("0_0")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.0)

        actual = Frac("0_01")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.001)

        actual = Frac("00_1")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.001)

        actual = Frac("1_00_1")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.1001)

        actual = Frac("0_341_00_1")
        assertThat(actual.value)
            .isWithin(TOLERANCE)
            .of(0.0341001)
    }

    companion object {
        private const val TOLERANCE = 0.000001
    }
}
