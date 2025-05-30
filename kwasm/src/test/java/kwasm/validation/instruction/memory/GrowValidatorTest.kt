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
class GrowValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun isInvalid_whenNoMemoryExists() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
        assertThrows(ValidationException::class.java) {
            "memory.grow".parseInstruction().validate(context)
        }
    }

    @Test
    fun isInvalid_whenStackIsEmpty() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
        val e = assertThrows(ValidationException::class.java) {
            "memory.grow".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat().contains("Grow requires an i32 at the top of the stack")
    }

    @Test
    fun isInvalid_whenStackContainsInvalidType() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I64)
        val e = assertThrows(ValidationException::class.java) {
            "memory.grow".parseInstruction().validate(context)
        }
        assertThat(e).hasMessageThat()
            .contains("Grow requires an i32 at the top of the stack, but i64 is present")
    }

    @Test
    fun valid() = parser.with {
        val context = ValidationContext.EMPTY_FUNCTION_BODY.withMemory()
            .pushStack(ValueType.I32)
        val resultContext = "memory.grow".parseInstruction().validate(context)
        assertThat(resultContext.stack).containsExactly(ValueType.I32)
    }
}
