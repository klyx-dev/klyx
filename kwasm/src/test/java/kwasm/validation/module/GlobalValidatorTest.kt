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
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GlobalValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifExpressionIsWrongType() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (global (mut i32) (i64.const 1))
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun invalid_ifExpressionIsNotConstant() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (global (mut i32) (i32.add (i32.const 1) (i32.const 2)))
                )
            """.trimIndent().parseModule().validate()
        }
    }
}
