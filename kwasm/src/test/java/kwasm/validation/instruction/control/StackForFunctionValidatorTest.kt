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
import kwasm.ast.Identifier
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.ValueType
import kwasm.validation.ValidationContext.Companion.EMPTY_FUNCTION_BODY
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StackForFunctionValidatorTest {
    private val functionTypeWithNone = FunctionType(
        emptyList(),
        listOf(
            Result(ValueType.F32)
        )
    )
    private val functionTypeWithTwo = FunctionType(
        listOf(
            Param(Identifier.Local(null, null), ValueType.I32),
            Param(Identifier.Local(null, null), ValueType.I32)
        ),
        listOf(
            Result(ValueType.F32)
        )
    )

    @Test
    fun invalid_whenStack_doesntHaveEnough() {
        assertThrows(ValidationException::class.java) {
            EMPTY_FUNCTION_BODY.validateStackForFunctionType(functionTypeWithTwo)
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Expected 2 item(s) in the stack, but only found 0")
        }

        assertThrows(ValidationException::class.java) {
            EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
                .validateStackForFunctionType(functionTypeWithTwo)
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Expected 2 item(s) in the stack, but only found 1")
        }
    }

    @Test
    fun invalid_whenTypesDontMatch() {
        assertThrows(ValidationException::class.java) {
            EMPTY_FUNCTION_BODY
                .pushStack(ValueType.I32)
                .pushStack(ValueType.F32)
                .validateStackForFunctionType(functionTypeWithTwo)
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Expected [i32, i32] at the top of the stack, but found [i32, f32]")
        }

        assertThrows(ValidationException::class.java) {
            EMPTY_FUNCTION_BODY
                .pushStack(ValueType.F32)
                .pushStack(ValueType.I32)
                .validateStackForFunctionType(functionTypeWithTwo)
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Expected [i32, i32] at the top of the stack, but found [f32, i32]")
        }

        assertThrows(ValidationException::class.java) {
            EMPTY_FUNCTION_BODY
                .pushStack(ValueType.F32)
                .pushStack(ValueType.F32)
                .validateStackForFunctionType(functionTypeWithTwo)
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Expected [i32, i32] at the top of the stack, but found [f32, f32]")
        }
    }

    @Test
    fun valid_whenStackEmpty_andFunctionHasNoParams() {
        val result = EMPTY_FUNCTION_BODY.validateStackForFunctionType(functionTypeWithNone)
        assertThat(result.stack).containsExactly(ValueType.F32)
    }

    @Test
    fun valid_whenStackFull_andFunctionHasNoParams() {
        val result = EMPTY_FUNCTION_BODY
            .pushStack(ValueType.I32)
            .pushStack(ValueType.I32)
            .pushStack(ValueType.I32)
            .validateStackForFunctionType(functionTypeWithNone)
        assertThat(result.stack)
            .containsExactly(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.F32)
    }

    @Test
    fun valid_whenStackFull_andFunctionHasMatchingParams() {
        var result = EMPTY_FUNCTION_BODY
            .pushStack(ValueType.I32)
            .pushStack(ValueType.I32)
            .pushStack(ValueType.I32)
            .validateStackForFunctionType(functionTypeWithTwo)
        assertThat(result.stack)
            .containsExactly(ValueType.I32, ValueType.F32)

        result = EMPTY_FUNCTION_BODY
            .pushStack(ValueType.F32)
            .pushStack(ValueType.I32)
            .pushStack(ValueType.I32)
            .validateStackForFunctionType(functionTypeWithTwo)
        assertThat(result.stack)
            .containsExactly(ValueType.F32, ValueType.F32)
    }
}
