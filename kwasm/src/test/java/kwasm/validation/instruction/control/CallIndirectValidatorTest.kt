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
import kwasm.ast.type.ElementType
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Limits
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.TableType
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
class CallIndirectValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifTableNotDefined() = parser.with {
        assertThrows(ValidationException::class.java) {
            "call_indirect 0".parseInstruction()
                .validate(EMPTY_FUNCTION_BODY)
        }.also {
            assertThat(it).hasMessageThat().contains("Expected table 0 to be defined")
        }
    }

    @Test
    fun invalid_ifTableWrongType() = parser.with {
        assertThrows(ValidationException::class.java) {
            "call_indirect 0".parseInstruction()
                .validate(
                    EMPTY_FUNCTION_BODY.copy(
                        tables = MutableAstNodeIndex<TableType>().apply {
                            prepend(TableType(Limits(0), ElementType.Illegal))
                        }
                    )
                )
        }.also {
            assertThat(it).hasMessageThat().contains("Expected table type to be funcref")
        }
    }

    @Test
    fun invalid_ifTypeUseNotFound_byIndex() = parser.with {
        assertThrows(ValidationException::class.java) {
            "call_indirect (type 0)".parseInstruction()
                .validate(CONTEXT_WITH_TABLE)
        }.also {
            assertThat(it).hasMessageThat().contains("Type with index 0 not found")
        }
    }

    @Test
    fun invalid_ifTypeUseNotFound_byParamsAndResults() = parser.with {
        assertThrows(ValidationException::class.java) {
            "call_indirect (param i32 i64) (result f32)".parseInstruction()
                .validate(CONTEXT_WITH_TABLE)
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Type with FunctionType [i32, i64] => [f32] not found")
        }
    }

    @Test
    fun invalid_ifTypeFound_andStack_doesntHaveRequiredParams() = parser.with {
        assertThrows(ValidationException::class.java) {
            "call_indirect (param i32) (result i64)".parseInstruction()
                .validate(CONTEXT_WITH_TABLE_AND_TYPE)
        }
    }

    @Test
    fun valid_ifTypeUseFound_andStackContains_requiredParams() = parser.with {
        val result = "call_indirect (type 0)"
            .parseInstruction()
            .validate(
                CONTEXT_WITH_TABLE_AND_TYPE
                    .pushStack(ValueType.I32)
                    .pushStack(ValueType.I32)
            )
        assertThat(result.stack).containsExactly(ValueType.I64)
    }

    companion object {
        private val CONTEXT_WITH_TABLE =
            EMPTY_FUNCTION_BODY.copy(
                tables = MutableAstNodeIndex<TableType>().apply {
                    prepend(TableType(Limits(0), ElementType.FunctionReference))
                }
            )

        private val CONTEXT_WITH_TABLE_AND_TYPE =
            CONTEXT_WITH_TABLE.copy(
                types = MutableAstNodeIndex<Type>().apply {
                    prepend(
                        Type(
                            null,
                            FunctionType(
                                listOf(
                                    Param(
                                        Identifier.Local(null, null),
                                        ValueType.I32
                                    )
                                ),
                                listOf(
                                    Result(ValueType.I64)
                                )
                            )
                        )
                    )
                }
            )
    }
}
