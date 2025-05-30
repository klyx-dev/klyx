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

package kwasm.validation.module

import kwasm.ParseRule
import kwasm.validation.ValidationContext.Companion.EMPTY_MODULE
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TableValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifTableType_isInvalid() = parser.with {
        assertThrows(Exception::class.java) {
            """
                (module
                    (table 0 4834534223 funcref)
                )
            """.trimIndent().parseModule().validate(EMPTY_MODULE)
        }
    }

    @Test
    fun valid_ifTableType_isValid() = parser.with {
        """
            (module
                (table 0 20 funcref)
            )
        """.trimIndent().parseModule().validate(EMPTY_MODULE)
    }
}
