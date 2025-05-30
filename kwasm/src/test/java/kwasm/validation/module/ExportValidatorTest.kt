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
class ExportValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifExternType_unsupported() = parser.with {
        assertThrows(Exception::class.java) {
            """
                (module
                    (export "foo" (unsupported $0))
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun invalid_ifFunctionNotFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (export "foo" (func 0))
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Function with index 0 not found in the module")
        }
    }

    @Test
    fun valid_function() = parser.with {
        """
            (module
                (func)
                (export "foo" (func 0))
            )
        """.trimIndent().parseModule().validate()
    }

    @Test
    fun invalid_ifTableNotFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (export "foo" (table 0))
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Table with index 0 not found in the module")
        }
    }

    @Test
    fun valid_table() = parser.with {
        """
            (module
                (table 0 1 funcref)
                (export "foo" (table 0))
            )
        """.trimIndent().parseModule().validate()
    }

    @Test
    fun invalid_ifMemoryNotFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (export "foo" (memory 0))
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Memory with index 0 not found in the module")
        }
    }

    @Test
    fun valid_memory() = parser.with {
        """
            (module
                (memory 1)
                (export "foo" (memory 0))
            )
        """.trimIndent().parseModule().validate()
    }

    @Test
    fun invalid_ifGlobalNotFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (export "foo" (global 0))
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Global with index 0 not found in the module")
        }
    }

    @Test
    fun valid_global() = parser.with {
        """
            (module
                (global (mut i32) (i32.const 0))
                (export "foo" (global 0))
            )
        """.trimIndent().parseModule().validate()
    }
}
