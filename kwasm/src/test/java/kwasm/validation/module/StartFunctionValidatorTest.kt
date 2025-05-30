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

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StartFunctionValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifStartFunction_notFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (start 0)
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat().contains("Function with index 0 not found in the module")
        }
    }

    @Test
    fun invalid_ifStartFunction_hasParams() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (start 0)
                    (func (param i32))
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Start function type expected to be [] => [], but is [i32] => []")
        }
    }

    @Test
    fun invalid_ifStartFunction_hasResult() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (start 0)
                    (func (result i32) (i32.const 0))
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Start function type expected to be [] => [], but is [] => [i32]")
        }
    }

    @Test
    fun valid_byIndex() = parser.with {
        """
            (module
                (func ${"$"}start)
                (start ${"$"}start)
            )
        """.trimIndent().parseModule().validate()
    }

    @Test
    fun valid_byInt() = parser.with {
        """
            (module
                (func ${"$"}start)
                (start 0)
            )
        """.trimIndent().parseModule().validate()
    }

    @Test
    fun valid_byImport() = parser.with {
        """
            (module
                (import "other" "module" (func $0)) 
                (start 0)
            )
        """.trimIndent().parseModule().validate()
    }
}
