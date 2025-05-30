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
class ImportValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifFunctionType_notFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (import "other" "module" (func (type 0)))
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun valid_typeFound_byInt() = parser.with {
        """
            (module
                (type (func (param i32)))
                (import "other" "module" (func (type 0)))
            )
        """.trimIndent().parseModule().validate()
    }

    @Test
    fun valid_typeFound_byId() = parser.with {
        """
            (module
                (type $0 (func (param i32)))
                (import "other" "module" (func (type $0)))
            )
        """.trimIndent().parseModule().validate()
    }

    @Test
    fun invalid_ifTableType_isInvalid() = parser.with {
        assertThrows(Exception::class.java) {
            """
                (module
                    (import "other" "module" (table 0 1 illegal))
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun valid_table() = parser.with {
        """
            (module
                (import "other" "module" (table 0 1 funcref))
            )
        """.trimIndent().parseModule().validate()
    }

    @Test
    fun invalid_ifMemoryType_isInvalid() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (import "other" "module" (memory 0 450000))
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun valid_memory() = parser.with {
        """
            (module
                (import "other" "module" (memory 0 1))
            )
        """.trimIndent().parseModule().validate()
    }

    @Test
    fun invalid_ifGlobalType_isInvalid() = parser.with {
        assertThrows(Exception::class.java) {
            """
                (module
                    (import "other" "module" (global (mut i16)))
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun valid_global() = parser.with {
        """
            (module
                (import "other" "module" (global (mut i32)))
            )
        """.trimIndent().parseModule().validate()
    }
}
