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
class LoadIntValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun i32_isInvalid_whenNoMemoryExists() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
        assertThrows(ValidationException::class.java) {
            "i32.load".parseInstruction().validate(context)
        }
    }

    @Test
    fun i32_isInvalid_whenStackIsEmpty() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
        val e = assertThrows(ValidationException::class.java) {
            "i32.load".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat().contains("Load expects an i32 at the top of the stack")
    }

    @Test
    fun i32_isInvalid_whenStackContainsInvalidType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I64)
        val e = assertThrows(ValidationException::class.java) {
            "i32.load".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains("Load expects an i32 at the top of the stack, but i64 was present")
    }

    @Test
    fun i32_isInvalid_whenAlignmentIsInvalid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        assertThrows(Exception::class.java) {
            "i32.load align=3".parseInstruction().validate(context)
        }
    }

    @Test
    fun i32_isValid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        val result = "i32.load".parseInstruction().validate(context)
        assertThat(result.stack).containsExactly(ValueType.I32)
    }

    @Test
    fun i32_8_isInvalid_whenNoMemoryExists() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
        assertThrows(ValidationException::class.java) {
            "i32.load8_s".parseInstruction().validate(context)
        }
    }

    @Test
    fun i32_8_isInvalid_whenStackIsEmpty() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
        val e = assertThrows(ValidationException::class.java) {
            "i32.load8_s".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat().contains("Load expects an i32 at the top of the stack")
    }

    @Test
    fun i32_8_isInvalid_whenStackContainsInvalidType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I64)
        val e = assertThrows(ValidationException::class.java) {
            "i32.load8_s".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains("Load expects an i32 at the top of the stack, but i64 was present")
    }

    @Test
    fun i32_8_isInvalid_whenAlignmentIsInvalid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        assertThrows(Exception::class.java) {
            "i32.load8_s align=3".parseInstruction().validate(context)
        }
    }

    @Test
    fun i32_8_isValid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        val result = "i32.load8_s".parseInstruction().validate(context)
        assertThat(result.stack).containsExactly(ValueType.I32)
    }

    @Test
    fun i32_16_isInvalid_whenNoMemoryExists() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
        assertThrows(ValidationException::class.java) {
            "i32.load16_s".parseInstruction().validate(context)
        }
    }

    @Test
    fun i32_16_isInvalid_whenStackIsEmpty() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
        val e = assertThrows(ValidationException::class.java) {
            "i32.load16_s".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat().contains("Load expects an i32 at the top of the stack")
    }

    @Test
    fun i32_16_isInvalid_whenStackContainsInvalidType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I64)
        val e = assertThrows(ValidationException::class.java) {
            "i32.load16_s".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains("Load expects an i32 at the top of the stack, but i64 was present")
    }

    @Test
    fun i32_16_isInvalid_whenAlignmentIsInvalid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        assertThrows(Exception::class.java) {
            "i32.load16_s align=3".parseInstruction().validate(context)
        }
    }

    @Test
    fun i32_16_isValid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        val result = "i32.load16_s".parseInstruction().validate(context)
        assertThat(result.stack).containsExactly(ValueType.I32)
    }

    @Test
    fun i64_isInvalid_whenNoMemoryExists() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
        assertThrows(ValidationException::class.java) {
            "i64.load".parseInstruction().validate(context)
        }
    }

    @Test
    fun i64_isInvalid_whenStackIsEmpty() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
        val e = assertThrows(ValidationException::class.java) {
            "i64.load".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat().contains("Load expects an i32 at the top of the stack")
    }

    @Test
    fun i64_isInvalid_whenStackContainsInvalidType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I64)
        val e = assertThrows(ValidationException::class.java) {
            "i64.load".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains("Load expects an i32 at the top of the stack, but i64 was present")
    }

    @Test
    fun i64_isInvalid_whenAlignmentIsInvalid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        assertThrows(Exception::class.java) {
            "i64.load align=3".parseInstruction().validate(context)
        }
    }

    @Test
    fun i64_isValid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        val result = "i64.load".parseInstruction().validate(context)
        assertThat(result.stack).containsExactly(ValueType.I64)
    }

    @Test
    fun i64_8_isInvalid_whenNoMemoryExists() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
        assertThrows(ValidationException::class.java) {
            "i64.load8_s".parseInstruction().validate(context)
        }
    }

    @Test
    fun i64_8_isInvalid_whenStackIsEmpty() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
        val e = assertThrows(ValidationException::class.java) {
            "i64.load8_s".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat().contains("Load expects an i32 at the top of the stack")
    }

    @Test
    fun i64_8_isInvalid_whenStackContainsInvalidType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I64)
        val e = assertThrows(ValidationException::class.java) {
            "i64.load8_s".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains("Load expects an i32 at the top of the stack, but i64 was present")
    }

    @Test
    fun i64_8_isInvalid_whenAlignmentIsInvalid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        assertThrows(Exception::class.java) {
            "i64.load8_s align=3".parseInstruction().validate(context)
        }
    }

    @Test
    fun i64_8_isValid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        val result = "i64.load8_s".parseInstruction().validate(context)
        assertThat(result.stack).containsExactly(ValueType.I64)
    }

    @Test
    fun i64_16_isInvalid_whenNoMemoryExists() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
        assertThrows(ValidationException::class.java) {
            "i64.load16_s".parseInstruction().validate(context)
        }
    }

    @Test
    fun i64_16_isInvalid_whenStackIsEmpty() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
        val e = assertThrows(ValidationException::class.java) {
            "i64.load16_s".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat().contains("Load expects an i32 at the top of the stack")
    }

    @Test
    fun i64_16_isInvalid_whenStackContainsInvalidType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I64)
        val e = assertThrows(ValidationException::class.java) {
            "i64.load16_s".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains("Load expects an i32 at the top of the stack, but i64 was present")
    }

    @Test
    fun i64_16_isInvalid_whenAlignmentIsInvalid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        assertThrows(Exception::class.java) {
            "i64.load16_s align=3".parseInstruction().validate(context)
        }
    }

    @Test
    fun i64_16_isValid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        val result = "i64.load16_s".parseInstruction().validate(context)
        assertThat(result.stack).containsExactly(ValueType.I64)
    }

    @Test
    fun i64_32_isInvalid_whenNoMemoryExists() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
        assertThrows(ValidationException::class.java) {
            "i64.load32_s".parseInstruction().validate(context)
        }
    }

    @Test
    fun i64_32_isInvalid_whenStackIsEmpty() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
        val e = assertThrows(ValidationException::class.java) {
            "i64.load32_s".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat().contains("Load expects an i32 at the top of the stack")
    }

    @Test
    fun i64_32_isInvalid_whenStackContainsInvalidType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I64)
        val e = assertThrows(ValidationException::class.java) {
            "i64.load32_s".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains("Load expects an i32 at the top of the stack, but i64 was present")
    }

    @Test
    fun i64_32_isInvalid_whenAlignmentIsInvalid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        assertThrows(Exception::class.java) {
            "i64.load32_s align=3".parseInstruction().validate(context)
        }
    }

    @Test
    fun i64_32_isValid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        val result = "i64.load32_s".parseInstruction().validate(context)
        assertThat(result.stack).containsExactly(ValueType.I64)
    }
}
