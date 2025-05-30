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

package kwasm.validation.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.ast.type.ValueType.F32
import kwasm.ast.type.ValueType.F64
import kwasm.ast.type.ValueType.I32
import kwasm.ast.type.ValueType.I64
import kwasm.validation.ValidationContext.Companion.EMPTY_FUNCTION_BODY
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParametricInstructionValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun drop_isInvalid_whenStackIsEmpty() = parser.with {
        val instruction = "drop".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY)
        }
        assertThat(e).hasMessageThat().contains("Drop expects a non-empty stack")
    }

    @Test
    fun drop_isValid_whenStackIsNonEmpty() = parser.with {
        val instruction = "drop".parseInstruction()
        val updatedContext = instruction.validate(
            EMPTY_FUNCTION_BODY.pushStack(I32)
        )

        assertThat(updatedContext.stack).isEmpty()
    }

    @Test
    fun select_isInvalid_whenStackIsEmpty() = parser.with {
        val instruction = "select".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY)
        }
        assertThat(e).hasMessageThat()
            .contains("Select expects at least three ValueTypes on the stack")
    }

    @Test
    fun select_isInvalid_whenStackHasOneType() = parser.with {
        val instruction = "select".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.pushStack(I32))
        }
        assertThat(e).hasMessageThat()
            .contains("Select expects at least three ValueTypes on the stack")
    }

    @Test
    fun select_isInvalid_whenStackHasTwoType() = parser.with {
        val instruction = "select".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(
                EMPTY_FUNCTION_BODY
                    .pushStack(I32)
                    .pushStack(I32)
            )
        }
        assertThat(e).hasMessageThat()
            .contains("Select expects at least three ValueTypes on the stack")
    }

    @Test
    fun select_isInvalid_whenStackTop_isNotI32() = parser.with {
        val instruction = "select".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(
                EMPTY_FUNCTION_BODY
                    .pushStack(I32)
                    .pushStack(I32)
                    .pushStack(F32)
            )
        }
        assertThat(e).hasMessageThat()
            .contains(
                "Select expects the ValueType at the top of the stack to be i32, but f32 was found"
            )
    }

    @Test
    fun select_isInvalid_when2ndAnd3rd_areNotSameType() = parser.with {
        val instruction = "select".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(
                EMPTY_FUNCTION_BODY
                    .pushStack(I64)
                    .pushStack(I32)
                    .pushStack(I32)
            )
        }
        assertThat(e).hasMessageThat()
            .contains(
                "Select expects the 2nd and 3rd ValueTypes to be the same, but i32 != i64"
            )
    }

    @Test
    fun select_isValid_whenStack_isValid() = parser.with {
        val instruction = "select".parseInstruction()

        instruction.validate(
            EMPTY_FUNCTION_BODY
                .pushStack(I32)
                .pushStack(I32)
                .pushStack(I32)
        ).also { assertThat(it.stack).containsExactly(I32) }

        instruction.validate(
            EMPTY_FUNCTION_BODY
                .pushStack(I64)
                .pushStack(I64)
                .pushStack(I32)
        ).also { assertThat(it.stack).containsExactly(I64) }

        instruction.validate(
            EMPTY_FUNCTION_BODY
                .pushStack(F32)
                .pushStack(F32)
                .pushStack(I32)
        ).also { assertThat(it.stack).containsExactly(F32) }

        instruction.validate(
            EMPTY_FUNCTION_BODY
                .pushStack(F64)
                .pushStack(F64)
                .pushStack(I32)
        ).also { assertThat(it.stack).containsExactly(F64) }
    }
}
