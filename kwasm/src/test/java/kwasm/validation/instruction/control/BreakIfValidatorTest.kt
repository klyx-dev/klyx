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
class BreakIfValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifLabel_doesntExist() = parser.with {
        assertThrows(ValidationException::class.java) {
            "br_if 0".parseInstruction().validate(EMPTY_FUNCTION_BODY)
        }

        assertThrows(ValidationException::class.java) {
            "br_if \$foo".parseInstruction().validate(EMPTY_FUNCTION_BODY)
        }
    }

    @Test
    fun invalid_ifI32_isNotAtopTheStack() = parser.with {
        assertThrows(ValidationException::class.java) {
            "br_if 0".parseInstruction()
                .validate(
                    BODY_WITH_LABEL
                        .pushStack(ValueType.I32)
                        .pushStack(ValueType.I64)
                )
        }.also { assertThat(it).hasMessageThat().contains("Expected i32 at the top of the stack") }
    }

    @Test
    fun invalid_ifExpectedType_isNotSecondInTheStack() = parser.with {
        assertThrows(ValidationException::class.java) {
            "br_if 0".parseInstruction()
                .validate(
                    BODY_WITH_LABEL
                        .pushStack(ValueType.I64)
                        .pushStack(ValueType.I32)
                )
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Expected i32 at the second position in the stack")
        }
    }

    @Test
    fun valid_ifStackOnlyHasI32_andLabelHasEmptyResultType() = parser.with {
        val result = "br_if 0"
            .parseInstruction()
            .validate(
                EMPTY_FUNCTION_BODY.prependLabel(ResultType(null))
                    .pushStack(ValueType.I32)
            )
        assertThat(result.stack).isEmpty()
    }

    @Test
    fun valid_ifStackFullWithI32Atop_andLabelHasEmptyResultType() = parser.with {
        val result = "br_if 0".parseInstruction()
            .validate(
                EMPTY_FUNCTION_BODY.prependLabel(ResultType(null))
                    .pushStack(ValueType.F32)
                    .pushStack(ValueType.I64)
                    .pushStack(ValueType.I32)
            )

        assertThat(result.stack).containsExactly(ValueType.F32, ValueType.I64)
    }

    @Test
    fun valid_ifStackSecond_matches_label() = parser.with {
        val result = "br_if 0".parseInstructions()
            .validate(
                BODY_WITH_LABEL.pushStack(ValueType.I32)
                    .pushStack(ValueType.I32)
            )

        assertThat(result.stack).containsExactly(ValueType.I32)
    }

    companion object {
        private val BODY_WITH_LABEL =
            EMPTY_FUNCTION_BODY.prependLabel(ResultType(Result(ValueType.I32)))
    }
}
