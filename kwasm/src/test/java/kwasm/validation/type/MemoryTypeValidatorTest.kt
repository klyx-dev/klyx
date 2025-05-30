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
import kwasm.format.text.type.parseMemoryType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MemoryTypeValidatorTest {
    @get:Rule val parser = ParseRule()

    @Test
    fun limitsOutsideRange_isInvalid() = parser.with {
        assertThrows(ValidationException::class.java) {
            "${MemoryTypeValidator.LIMITS_RANGE + 1}".tokenize()
                .parseMemoryType(0).astNode
                .validate(ValidationContext.EMPTY_MODULE)
        }
    }

    @Test
    fun invalidLimits_isInvalid() = parser.with {
        assertThrows(ValidationException::class.java) {
            "1 0".tokenize()
                .parseMemoryType(0).astNode
                .validate(ValidationContext.EMPTY_MODULE)
        }
    }

    @Test
    fun validLimits_isValid() = parser.with {
        "0 1".tokenize()
            .parseMemoryType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)
    }

    @Test
    fun maxValidLimits_isValid() = parser.with {
        "${MemoryTypeValidator.LIMITS_RANGE} ${MemoryTypeValidator.LIMITS_RANGE}".tokenize()
            .parseMemoryType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)
    }
}
