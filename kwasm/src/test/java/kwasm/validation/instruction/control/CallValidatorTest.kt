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
import kwasm.ast.module.TypeUse
import kwasm.ast.type.Param
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
class CallValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifFunctionNotFound() = parser.with {
        assertThrows(ValidationException::class.java) {
            "call \$myFunc".parseInstruction().validate(EMPTY_FUNCTION_BODY)
        }.also {
            assertThat(it).hasMessageThat().contains("Function with index: \$myFunc not found")
        }
    }

    @Test
    fun invalid_ifFunctionParams_notAtopStack() = parser.with {
        assertThrows(ValidationException::class.java) {
            "call \$myFunc".parseInstruction().validate(CONTEXT_WITH_FUNCTION_TWO_PARAMS)
        }

        assertThrows(ValidationException::class.java) {
            "call \$myFunc".parseInstruction()
                .validate(
                    CONTEXT_WITH_FUNCTION_TWO_PARAMS
                        .pushStack(ValueType.I32)
                )
        }

        assertThrows(ValidationException::class.java) {
            "call \$myFunc".parseInstruction()
                .validate(
                    CONTEXT_WITH_FUNCTION_TWO_PARAMS
                        .pushStack(ValueType.I64)
                )
        }
    }

    @Test
    fun invalid_ifFunctionParams_wrongOrder() = parser.with {
        assertThrows(ValidationException::class.java) {
            "call \$myFunc".parseInstruction()
                .validate(
                    CONTEXT_WITH_FUNCTION_TWO_PARAMS
                        .pushStack(ValueType.I64)
                        .pushStack(ValueType.I32)
                )
        }
    }

    @Test
    fun valid_ifFunctionNoParams_emptyStack() = parser.with {
        val result = "call \$myFunc".parseInstruction()
            .validate(CONTEXT_WITH_FUNCTION_NO_PARAMS)
        assertThat(result.stack).isEmpty()
    }

    @Test
    fun valid_ifFunctionNoParams_fullStack() = parser.with {
        val result = "call \$myFunc".parseInstruction()
            .validate(
                CONTEXT_WITH_FUNCTION_NO_PARAMS
                    .pushStack(ValueType.I32)
                    .pushStack(ValueType.I64)
            )
        assertThat(result.stack).containsExactly(ValueType.I32, ValueType.I64)
    }

    @Test
    fun valid_ifFunctionParams_correctOrder() = parser.with {
        val result = "call \$myFunc".parseInstruction()
            .validate(
                CONTEXT_WITH_FUNCTION_TWO_PARAMS
                    .pushStack(ValueType.I32)
                    .pushStack(ValueType.I64)
            )
        assertThat(result.stack).isEmpty()
    }

    companion object {
        private val CONTEXT_WITH_FUNCTION_NO_PARAMS =
            EMPTY_FUNCTION_BODY.copy(
                functions = MutableAstNodeIndex<TypeUse>().apply {
                    this[Identifier.Function("\$myFunc")] =
                        TypeUse(null, emptyList(), emptyList())
                }
            )
        private val CONTEXT_WITH_FUNCTION_TWO_PARAMS =
            EMPTY_FUNCTION_BODY.copy(
                functions = MutableAstNodeIndex<TypeUse>().apply {
                    this[Identifier.Function("\$myFunc")] =
                        TypeUse(
                            null,
                            listOf(
                                Param(Identifier.Local("$0"), ValueType.I32),
                                Param(Identifier.Local("$1"), ValueType.I64)
                            ),
                            emptyList()
                        )
                }
            )
    }
}
