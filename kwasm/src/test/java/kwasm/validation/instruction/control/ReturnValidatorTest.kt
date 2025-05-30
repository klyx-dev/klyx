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
import kwasm.ast.type.Result
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import kwasm.validation.ValidationContext.Companion.EMPTY_FUNCTION_BODY
import kwasm.validation.ValidationException
import kwasm.validation.instruction.validate
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReturnValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifReturnInContext_isNull() = parser.with {
        assertThrows(ValidationException::class.java) {
            "return".parseInstruction().validate(EMPTY_FUNCTION_BODY)
        }.also {
            assertThat(it).hasMessageThat().contains("Return type must not be absent")
        }
    }

    @Test
    fun invalid_ifReturnType_isNotAtopStack() = parser.with {
        assertThrows(ValidationException::class.java) {
            "return".parseInstruction()
                .validate(
                    EMPTY_FUNCTION_BODY.copy(
                        returnType = ResultType(Result(ValueType.I64))
                    )
                )
        }.also {
            assertThat(it).hasMessageThat().contains("Expected i64 at the top of the stack")
        }
    }

    @Test
    fun valid_ifReturnType_isEmpty_andStackEmpty() = parser.with {
        "return".parseInstruction()
            .validate(
                EMPTY_FUNCTION_BODY.copy(
                    returnType = ResultType(null)
                )
            )
    }

    @Test
    fun valid_ifReturnType_isEmpty_andStackFull() = parser.with {
        val result = "return".parseInstruction()
            .validate(
                EMPTY_FUNCTION_BODY.copy(
                    returnType = ResultType(null)
                ).pushStack(ValueType.I32).pushStack(ValueType.I64)
            )
        assertThat(result.stack).containsExactly(ValueType.I32, ValueType.I64)
    }

    @Test
    fun valid_ifReturnType_matchesTopOfStack() = parser.with {
        val result = "return".parseInstruction()
            .validate(
                EMPTY_FUNCTION_BODY.copy(
                    returnType = ResultType(Result(ValueType.I64))
                ).pushStack(ValueType.I32).pushStack(ValueType.I64)
            )
        assertThat(result.stack).containsExactly(ValueType.I32, ValueType.I64)
    }
}
