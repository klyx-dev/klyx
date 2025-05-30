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

import kwasm.ParseRule
import kwasm.format.ParseException
import kwasm.format.text.type.parseTableType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class TableTypeValidatorTest {
    @get:Rule val parser = ParseRule()

    @Test
    fun limitsOutsideRange_isInvalid() = parser.with {
        assertThrows(ParseException::class.java) {
            "${UInt.MAX_VALUE.toULong() + 1.toULong()} funcref".tokenize()
                .parseTableType(0).astNode
                .validate(ValidationContext.EMPTY_MODULE)
        }
    }

    @Test
    fun invalidLimits_isInvalid() = parser.with {
        assertThrows(ValidationException::class.java) {
            "1 0 funcref".tokenize()
                .parseTableType(0).astNode
                .validate(ValidationContext.EMPTY_MODULE)
        }
    }

    @Test
    fun validLimits_isValid() = parser.with {
        "0 1 funcref".tokenize()
            .parseTableType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)
    }

    @Test
    fun maxValidLimits_isValid() = parser.with {
        "${TableTypeValidator.LIMITS_RANGE} ${TableTypeValidator.LIMITS_RANGE} funcref".tokenize()
            .parseTableType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)
    }
}
