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
class ElementSegmentValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifTableNotDefined() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (elem (offset (i32.const 0)) ${"$"}foo)
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat().contains("Table with index 0 not found in the module")
        }
    }

    @Test
    fun invalid_ifTableNotFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (table 0 10 funcref)
                    (elem ${"$"}bar (offset (i32.const 0)) ${"$"}foo)
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat().contains("Table with index \$bar not found in the module")
        }
    }

    @Test
    fun invalid_ifTableNotFuncref() = parser.with {
        assertThrows(Exception::class.java) {
            """
                (module
                    (table 0 10 illegal)
                    (elem (offset (i32.const 0)) ${"$"}foo)
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun invalid_ifOffset_wrongType() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (table 0 10 funcref)
                    (elem (offset (i64.const 0)) ${"$"}foo)
                    (func ${"$"}foo)
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun invalid_ifOffset_nonConstant() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (table 0 10 funcref)
                    (elem (offset (i32.add (i32.const 0) (i32.const 1))) ${"$"}foo)
                    (func ${"$"}foo)
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun invalid_ifFunctionNotFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (table 0 10 funcref)
                    (elem (offset (i32.const 0)) ${"$"}foo)
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Function with index \$foo not found in the module")
        }
    }

    @Test
    fun invalid_ifOneFunctionNotFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (table 0 10 funcref)
                    (elem (offset (i32.const 0)) 0 ${"$"}foo)
                    (func)
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Function with index \$foo not found in the module")
        }
    }

    @Test
    fun valid() = parser.with {
        """
            (module
                (table 0 10 funcref)
                (elem (offset (i32.const 0)) 0 ${"$"}foo)
                (func)
                (func ${"$"}foo)
            )
        """.trimIndent().parseModule().validate()
    }
}
