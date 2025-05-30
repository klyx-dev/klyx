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
class ModuleValidatorTest {
    @get:Rule val parser = ParseRule()

    @Test
    fun moduleWithZeroMemoriesAndZeroTables_isValid() = parser.with {
        """
            (module)
        """.parseModule().validate()
    }

    @Test
    fun moduleWithOneTable_isValid() = parser.with {
        """
            (module
                (table 0 1 funcref)
            )
        """.parseModule().validate()
    }

    @Test
    fun moduleWithOneMemory_isValid() = parser.with {
        """
            (module
                (memory 0 1)
            )
        """.parseModule().validate()
    }

    @Test
    fun moreThanOne_table_isInvalid() = parser.with {
        val e = assertThrows(ValidationException::class.java) {
            """
                (module
                    (table $1 0 1 funcref)
                    (table $2 0 1 funcref)
                )
            """.parseModule().validate()
        }

        assertThat(e).hasMessageThat()
            .contains("Modules are not allowed to include more than one table")
    }

    @Test
    fun moreThanOne_memory_isInvalid() = parser.with {
        val e = assertThrows(ValidationException::class.java) {
            """
                (module
                    (memory $1 0 1)
                    (memory $2 0 1)
                )
            """.parseModule().validate()
        }

        assertThat(e).hasMessageThat()
            .contains("Modules are not allowed to include more than one memory")
    }

    @Test
    fun uniqueExports_areValid() = parser.with {
        """
            (module
                (func $0 (export "foo"))
            )
        """.parseModule().validate()

        """
            (module
                (func $0 (export "foo") (export "bar"))
            )
        """.parseModule().validate()
    }

    @Test
    fun duplicateExports_areInvalid() = parser.with {
        val e = assertThrows(ValidationException::class.java) {
            """
                (module
                    (func)
                    (table 0 1 funcref)
                    (export "foo" (func 0))
                    (export "foo" (table 0))
                    (export "bar" (func 0))
                )
            """.parseModule().validate()
        }

        assertThat(e).hasMessageThat()
            .contains("The following export names are used more than once: [foo]")
    }
}
