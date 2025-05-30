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
import kwasm.ast.type.ValueType
import kwasm.validation.ValidationContext.Companion.EMPTY_FUNCTION_BODY
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InstructionSequenceValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun emptySequence_isValid_withNoRequiredEndStack() = parser.with {
        "".parseInstructions().validate(EMPTY_FUNCTION_BODY)
            .also { assertThat(it.stack).isEmpty() }

        "".parseInstructions().validate(
            EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
        ).also {
            assertThat(it.stack).containsExactly(ValueType.I32)
        }

        "".parseInstructions().validate(
            EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
                .pushStack(ValueType.I64)
        ).also {
            assertThat(it.stack).containsExactly(ValueType.I32, ValueType.I64).inOrder()
        }

        "".parseInstructions().validate(
            EMPTY_FUNCTION_BODY.pushStack(ValueType.I32)
                .pushStack(ValueType.I64)
                .pushStack(ValueType.F32)
                .pushStack(ValueType.F64)
        ).also {
            assertThat(it.stack)
                .containsExactly(ValueType.I32, ValueType.I64, ValueType.F32, ValueType.F64)
                .inOrder()
        }
    }

    @Test
    fun emptySequence_isValid_whenInputStack_equalsRequiredStack_strict() = parser.with {
        val inStack = listOf(ValueType.I32, ValueType.F32, ValueType.I64, ValueType.F64)
        "".parseInstructions().validate(
            EMPTY_FUNCTION_BODY.copy(stack = inStack),
            requiredEndStack = inStack,
            strictEndStackMatchRequired = true
        )
    }

    @Test
    fun emptySequence_isValid_whenInputStack_endsWithRequiredStack_permissive() = parser.with {
        val inStack = listOf(ValueType.I32, ValueType.F32, ValueType.I64, ValueType.F64)
        "".parseInstructions().validate(
            EMPTY_FUNCTION_BODY.copy(stack = inStack),
            requiredEndStack = inStack.takeLast(2),
            strictEndStackMatchRequired = false
        )
    }

    @Test
    fun sequence_isValid_withNoRequiredEndStack() = parser.with {
        "(i32.add (i32.const 1) (i32.const 2))".parseInstructions()
            .validate(EMPTY_FUNCTION_BODY)
            .also { assertThat(it.stack).containsExactly(ValueType.I32) }
    }

    @Test
    fun sequence_isValid_withRequiredEndStack_strict() = parser.with {
        "(i32.add (i32.const 1) (i32.const 2))".parseInstructions()
            .validate(
                EMPTY_FUNCTION_BODY,
                requiredEndStack = listOf(ValueType.I32),
                strictEndStackMatchRequired = true
            )
            .also { assertThat(it.stack).containsExactly(ValueType.I32) }

        "i32.add".parseInstructions()
            .validate(
                EMPTY_FUNCTION_BODY.pushStack(ValueType.I32).pushStack(ValueType.I32),
                requiredEndStack = listOf(ValueType.I32),
                strictEndStackMatchRequired = true
            ).also { assertThat(it.stack).containsExactly(ValueType.I32) }
    }

    @Test
    fun sequence_isInvalid_whenInputDoesntMatchInstructionRequirements() = parser.with {
        assertThrows(ValidationException::class.java) {
            "i32.add".parseInstructions().validate(EMPTY_FUNCTION_BODY)
        }
    }

    // The following tests verify the end stack behavior for all instruction sequences, regardless
    // of the fact that they are empty sequences in the test cases.

    @Test
    fun sequence_isInvalid_whenInputStack_doesntEqualRequiredStack_strict() = parser.with {
        val inStack = listOf(ValueType.I32, ValueType.F32, ValueType.I64, ValueType.F64)

        assertThrows(ValidationException::class.java) {
            "".parseInstructions().validate(
                EMPTY_FUNCTION_BODY.copy(stack = inStack.takeLast(2)),
                requiredEndStack = inStack,
                strictEndStackMatchRequired = true
            )
        }.also { e ->
            assertThat(e).hasMessageThat()
                .contains(
                    "Required end stack is: [i32, f32, i64, f64], but instruction sequence " +
                        "results in: [i64, f64]"
                )
        }

        assertThrows(ValidationException::class.java) {
            "".parseInstructions().validate(
                EMPTY_FUNCTION_BODY.copy(stack = inStack),
                requiredEndStack = inStack.takeLast(2),
                strictEndStackMatchRequired = true
            )
        }.also { e ->
            assertThat(e).hasMessageThat()
                .contains(
                    "Strictly required end stack is: [i64, f64], but instruction " +
                        "sequence results in: [i32, f32, i64, f64]"
                )
        }
    }

    @Test
    fun sequence_isInvalid_whenInputStack_doesntEndWithRequired_permissive() = parser.with {
        assertThrows(ValidationException::class.java) {
            "".parseInstructions().validate(
                EMPTY_FUNCTION_BODY.copy(stack = listOf(ValueType.I32, ValueType.I64)),
                requiredEndStack = listOf(ValueType.I32, ValueType.F64),
                strictEndStackMatchRequired = false
            )
        }.also { e ->
            assertThat(e).hasMessageThat()
                .contains(
                    "Required end stack is: [i32, f64], but instruction sequence results in: " +
                        "[i32, i64]"
                )
        }
    }
}
