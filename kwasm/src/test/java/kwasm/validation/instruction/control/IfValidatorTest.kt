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

package kwasm.validation.instruction.control

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.ast.Identifier
import kwasm.ast.module.Type
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.ValueType
import kwasm.ast.util.MutableAstNodeIndex
import kwasm.validation.ValidationContext.Companion.EMPTY_FUNCTION_BODY
import kwasm.validation.ValidationException
import kwasm.validation.instruction.validate
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IfValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifTopOfStack_isNotI32() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
            (if 
                (then (nop))
            )
            """.trimIndent()
                .parseInstructions()
                .validate(EMPTY_FUNCTION_BODY.pushStack(ValueType.I64))
        }.also { assertThat(it).hasMessageThat().contains("Expected i32 at the top of the stack") }

        assertThrows(ValidationException::class.java) {
            """
            (if 
                (then (nop))
            )
            """.trimIndent()
                .parseInstructions()
                .validate(EMPTY_FUNCTION_BODY.pushStack(ValueType.F32))
        }.also { assertThat(it).hasMessageThat().contains("Expected i32 at the top of the stack") }

        assertThrows(ValidationException::class.java) {
            """
            (if 
                (then (nop))
            )
            """.trimIndent()
                .parseInstructions()
                .validate(EMPTY_FUNCTION_BODY.pushStack(ValueType.F64))
        }.also { assertThat(it).hasMessageThat().contains("Expected i32 at the top of the stack") }
    }

    @Test
    fun invalid_ifInstructions_finishWithWrongType_noneExpected() = parser.with {
        assertThrows(ValidationException::class.java) {
            "(if (then (i32.const 1)))"
                .parseInstructions()
                .validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_ifInstructions_finishWithWrongType_ofIndex_noneExpected() = parser.with {
        assertThrows(ValidationException::class.java) {
            "(if (type 0) (then (i32.const 1)))"
                .parseInstructions()
                .validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_ifInstructions_finishWithWrongType() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
            (if (result i64) 
                (then (i32.const 1))
            )
            """.trimIndent().parseInstructions().validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_ifInstructions_finishWithWrongType_ofIndex() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
            (if (type 1) 
                (then (i64.const 1))
            )
            """.trimIndent().parseInstructions().validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_ifInstructions_finishWithWrongType_expected_nonePresent() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
            (if (result i64) 
                (then (nop))
            )
            """.trimIndent().parseInstructions().validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_ifInstructions_finishWithWrongType_ofIndex_expected_nonePresent() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
            (if (type 1) 
                (then (nop))
            )
            """.trimIndent().parseInstructions().validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_elseInstructions_finishWithWrongType_noneExpected() = parser.with {
        assertThrows(ValidationException::class.java) {
            "(if (then (nop)) (else (i32.const 1)))"
                .parseInstructions()
                .validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_elseInstructions_finishWithWrongType_ofIndex_noneExpected() = parser.with {
        assertThrows(ValidationException::class.java) {
            "(if (type 0) (then (nop)) (else (i32.const 1)))"
                .parseInstructions()
                .validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_elseInstructions_finishWithWrongType() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
            (if (result i64) 
                (then (i64.const 1))
                (else (i32.const 1))
            )
            """.trimIndent().parseInstructions().validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_elseInstructions_finishWithWrongType_ofIndex() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
            (if (type 1) 
                (then (i32.const 1))
                (else (i64.const 1))
            )
            """.trimIndent().parseInstructions().validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_elseInstructions_finishWithWrongType_expected_nonePresent() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
            (if (result i64) 
                (then (i64.const 1))
                (else (nop))
            )
            """.trimIndent().parseInstructions().validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun invalid_elseInstructions_finishWithWrongType_atIndex_expected_nonePresent() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
            (if (type 1) 
                (then (i32.const 1))
                (else (nop))
            )
            """.trimIndent().parseInstructions().validate(STARTER_CONTEXT)
        }
    }

    @Test
    fun valid_noExpectedResult() = parser.with {
        var result =
            """
            (if
                (then (nop))
                (else (nop))
            )
            """.trimIndent().parseInstructions().validate(STARTER_CONTEXT)
        assertThat(result.stack).isEmpty()

        result =
            """
            (if
                (then 
                    (i32.add (i32.const 0) (i32.const 1))
                    (drop)
                )
                (else (nop)
                    (i32.add (i32.const 2) (i32.const 3))
                    (drop)
                )
            )
            """.trimIndent()
                .parseInstructions()
                .validate(STARTER_CONTEXT)
        assertThat(result.stack).isEmpty()
    }

    @Test
    fun valid_noExpectedResult_withIndex() = parser.with {
        var result =
            """
            (if (type 0)
                (then (nop))
                (else (nop))
            )
            """.trimIndent().parseInstructions().validate(STARTER_CONTEXT)
        assertThat(result.stack).isEmpty()

        result =
            """
            (if (type 0)
                (then 
                    (i32.add (i32.const 0) (i32.const 1))
                    (drop)
                )
                (else (nop)
                    (i32.add (i32.const 2) (i32.const 3))
                    (drop)
                )
            )
            """.trimIndent()
                .parseInstructions()
                .validate(STARTER_CONTEXT)
        assertThat(result.stack).isEmpty()
    }

    @Test
    fun valid_expectedResult() = parser.with {
        val result =
            """
            (if (result f32)
                (then (f32.const 0.0))
                (else (f32.const 1.0))
            )
            """.trimIndent()
                .parseInstructions()
                .validate(STARTER_CONTEXT)
        assertThat(result.stack).containsExactly(ValueType.F32)
    }

    @Test
    fun valid_expectedResult_withIndex() = parser.with {
        val result =
            """
            (if (type 1) 
                (then (i32.const 0))
                (else (i32.const 1))
            )
            """.trimIndent()
                .parseInstructions()
                .validate(STARTER_CONTEXT)
        assertThat(result.stack).containsExactly(ValueType.I32)
    }

    @Test
    fun valid_nested() = parser.with {
        val result =
            """
            (if (result i32)
                (then 
                    (i32.const 0)
                    (if (result f32)
                        (then f32.const 1.0)
                    )
                    drop
                    i32.const 1
                )
                (else 
                    (i32.const 1)
                    (if (result f32)
                        (then (f32.const 1.0))
                        (else (f32.const 0.0))
                    )
                    drop
                    i32.const 1
                )
            )
            """.trimIndent()
                .parseInstructions()
                .validate(STARTER_CONTEXT)
        assertThat(result.stack).containsExactly(ValueType.I32)
    }

    companion object {
        private val STARTER_CONTEXT = EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
            .copy(
                types = MutableAstNodeIndex<Type>().also {
                    it += Type(null, FunctionType(emptyList(), emptyList()))
                    it += Type(null, FunctionType(emptyList(), listOf(Result(ValueType.I32))))
                    it += Type(
                        null,
                        FunctionType(
                            listOf(Param(Identifier.Local(null, null), ValueType.I32)),
                            emptyList()
                        )
                    )
                }
            )
    }
}
