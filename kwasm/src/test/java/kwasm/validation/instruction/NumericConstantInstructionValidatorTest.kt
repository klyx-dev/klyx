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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NumericConstantInstructionValidatorTest {
    @get:Rule val parser = ParseRule()

    @Test
    fun validates_i32Const() = parser.with {
        val outContext = "i32.const 0".parseInstruction().validate(EMPTY_FUNCTION_BODY)
        assertThat(outContext.stack).containsExactly(ValueType.I32).inOrder()
    }

    @Test
    fun validates_i64Const() = parser.with {
        val outContext = "i64.const 0".parseInstruction().validate(EMPTY_FUNCTION_BODY)
        assertThat(outContext.stack).containsExactly(ValueType.I64).inOrder()
    }

    @Test
    fun validates_f32Const() = parser.with {
        val outContext = "f32.const 0.0".parseInstruction().validate(EMPTY_FUNCTION_BODY)
        assertThat(outContext.stack).containsExactly(ValueType.F32).inOrder()
    }

    @Test
    fun validates_f64Const() = parser.with {
        val outContext = "f64.const 0.0".parseInstruction().validate(EMPTY_FUNCTION_BODY)
        assertThat(outContext.stack).containsExactly(ValueType.F64).inOrder()
    }
}
