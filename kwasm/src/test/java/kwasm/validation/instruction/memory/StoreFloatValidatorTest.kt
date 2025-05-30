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

package kwasm.validation.instruction.memory

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.ast.type.ValueType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationException
import kwasm.validation.instruction.validate
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StoreFloatValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun f32_isInvalid_whenMemoryDoesntExist() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY
            .pushStack(ValueType.I32)
            .pushStack(ValueType.F32)
        assertThrows(ValidationException::class.java) {
            "f32.store".parseInstruction().validate(context)
        }
    }

    @Test
    fun f32_isInvalid_whenStackIsEmpty() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY
            .withMemory()
        assertThrows(ValidationException::class.java) {
            "f32.store".parseInstruction().validate(context)
        }
    }

    @Test
    fun f32_isInvalid_whenStackTop_isWrongType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY
            .withMemory()
            .pushStack(ValueType.I32)
            .pushStack(ValueType.F64)
        val e = assertThrows(ValidationException::class.java) {
            "f32.store".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains(
                "Store requires that the top of the stack has the same type as the instruction type"
            )
    }

    @Test
    fun f32_isInvalid_whenStackSecond_isWrongType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY
            .withMemory()
            .pushStack(ValueType.I64)
            .pushStack(ValueType.F32)
        val e = assertThrows(ValidationException::class.java) {
            "f32.store".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains(
                "Store requires an i32 at the second position in the stack, but i64 is present"
            )
    }

    @Test
    fun f32_isValid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY
            .withMemory()
            .pushStack(ValueType.I32)
            .pushStack(ValueType.F32)
        val result = "f32.store".parseInstruction().validate(context)
        assertThat(result.stack).isEmpty()
    }

    @Test
    fun f64_isInvalid_whenMemoryDoesntExist() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY
            .pushStack(ValueType.I32)
            .pushStack(ValueType.F64)
        assertThrows(ValidationException::class.java) {
            "f64.store".parseInstruction().validate(context)
        }
    }

    @Test
    fun f64_isInvalid_whenStackIsEmpty() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY
            .withMemory()
        assertThrows(ValidationException::class.java) {
            "f64.store".parseInstruction().validate(context)
        }
    }

    @Test
    fun f64_isInvalid_whenStackTop_isWrongType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY
            .withMemory()
            .pushStack(ValueType.I32)
            .pushStack(ValueType.F32)
        val e = assertThrows(ValidationException::class.java) {
            "f64.store".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains(
                "Store requires that the top of the stack has the same type as the instruction type"
            )
    }

    @Test
    fun f64_isInvalid_whenStackSecond_isWrongType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY
            .withMemory()
            .pushStack(ValueType.I64)
            .pushStack(ValueType.F64)
        val e = assertThrows(ValidationException::class.java) {
            "f64.store".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains(
                "Store requires an i32 at the second position in the stack, but i64 is present"
            )
    }

    @Test
    fun f64_isValid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY
            .withMemory()
            .pushStack(ValueType.I32)
            .pushStack(ValueType.F64)
        val result = "f64.store".parseInstruction().validate(context)
        assertThat(result.stack).isEmpty()
    }
}
