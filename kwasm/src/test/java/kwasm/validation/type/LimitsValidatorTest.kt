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

package kwasm.validation.type

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.format.text.type.parseLimits
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LimitsValidatorTest {
    @get:Rule val parser = ParseRule()

    @Test
    fun minOnly_min_lessThan_range_isValid() = parser.with {
        "10".tokenize()
            .parseLimits(0).astNode
            .validate(11, ValidationContext.EMPTY_MODULE)
    }

    @Test
    fun minOnly_min_equalTo_range_isValid() = parser.with {
        "10".tokenize()
            .parseLimits(0).astNode
            .validate(10, ValidationContext.EMPTY_MODULE)
    }

    @Test
    fun minOnly_min_greaterThan_range_isInvalid() = parser.with {
        val e = assertThrows(ValidationException::class.java) {
            "10".tokenize()
                .parseLimits(0).astNode
                .validate(1L, ValidationContext.EMPTY_MODULE)
        }
        assertThat(e).hasMessageThat()
            .contains("Limits' min value of 10 must not be greater than 1")
    }

    @Test
    fun minMax_both_lessThan_range_isValid() = parser.with {
        "10 11".tokenize()
            .parseLimits(0).astNode
            .validate(12, ValidationContext.EMPTY_MODULE)
    }

    @Test
    fun minMax_both_equalTo_range_isValid() = parser.with {
        "10 10".tokenize()
            .parseLimits(0).astNode
            .validate(10, ValidationContext.EMPTY_MODULE)
    }

    @Test
    fun minMax_min_greaterThan_max_isInvalid() = parser.with {
        val e = assertThrows(ValidationException::class.java) {
            "10 9".tokenize()
                .parseLimits(0).astNode
                .validate(10L, ValidationContext.EMPTY_MODULE)
        }
        assertThat(e).hasMessageThat()
            .contains("Limits' max value must be greater-than or equal to its min")
    }

    @Test
    fun minMax_max_greaterThan_range_isInvalid() = parser.with {
        val e = assertThrows(ValidationException::class.java) {
            "10 12".tokenize()
                .parseLimits(0).astNode
                .validate(11L, ValidationContext.EMPTY_MODULE)
        }
        assertThat(e).hasMessageThat()
            .contains("Limits' max value of 12 must not be greater than 11")
    }
}
