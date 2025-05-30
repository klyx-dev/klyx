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

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class WasmFunctionValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifTypeUseNotFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (func (type 0)
                        (return 
                            (i32.add (i32.const 1) (i32.const 3))
                        )
                    )
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun invalid_ifBodyReturnType_mismatchWith_expectedReturnType() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (func ${"$"}increment (param i32) (result i64)
                        (i32.add (local.get 0) (i32.const 1))
                        return
                    )
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun valid_withDefinedType() = parser.with {
        """
            (module
                (type ${"$"}myType (func (param $0 i32) (result i32)))
                (func (type ${"$"}myType)
                    (i32.add (local.get $0) (i32.const 0))
                    (return)
                )
            )
        """.trimIndent().parseModule().validate()
    }

    @Test
    fun valid_withImplicitType() = parser.with {
        """
            (module
                (func (param i32 i32 i32) (result i32)
                    (i32.mul 
                        (i32.add (local.get 0) (local.get 1))
                        (local.get 2)
                    )
                )
            )
        """.trimIndent()
    }
}
