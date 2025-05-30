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
import kwasm.ast.instruction.NumericInstruction
import kwasm.ast.type.ValueType
import kwasm.validation.ValidationContext.Companion.EMPTY_FUNCTION_BODY
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class NumericInstructionValidatorTest(private val source: String) {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun valid_stackForInputs_isValid() = parser.with {
        val instruction = source.parseInstruction()
        val (expectedIns, expectedOut) = (instruction as NumericInstruction).getExpectedInsAndOuts()
        val initialContext = EMPTY_FUNCTION_BODY.copy(stack = expectedIns)
        val expectedContext = EMPTY_FUNCTION_BODY.copy(stack = listOf(expectedOut))

        val actualContext = instruction.validate(initialContext)
        assertThat(actualContext.stack).containsExactlyElementsIn(expectedContext.stack)
    }

    @Test
    fun invalid_stackForInputs_isInvalid() = parser.with {
        val instruction = source.parseInstruction()
        val (expectedIns, _) = (instruction as NumericInstruction).getExpectedInsAndOuts()

        // nothing in the stack
        var e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY)
        }
        assertThat(e).hasMessageThat().contains(
            "Instruction requires top of stack to have args: $expectedIns, but at this point " +
                "the stack has []"
        )

        // wrong items in the stack
        val wrongIns = expectedIns.map {
            when (it) {
                ValueType.I32 -> ValueType.F32
                ValueType.I64 -> ValueType.F64
                ValueType.F32 -> ValueType.I32
                ValueType.F64 -> ValueType.I64
            }
        }
        e = assertThrows(ValidationException::class.java) {
            instruction.validate(
                wrongIns.fold(EMPTY_FUNCTION_BODY) { context, type -> context.pushStack(type) }
            )
        }
        assertThat(e).hasMessageThat().contains(
            "Instruction requires top of stack to have args: $expectedIns, but at this point " +
                "the stack has $wrongIns"
        )
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun parameters(): List<String> = listOf(
            "i32.clz",
            "i32.ctz",
            "i32.popcnt",
            "i32.add",
            "i32.sub",
            "i32.mul",
            "i32.div_s",
            "i32.div_u",
            "i32.rem_s",
            "i32.rem_u",
            "i32.and",
            "i32.or",
            "i32.xor",
            "i32.shl",
            "i32.shr_s",
            "i32.shr_u",
            "i32.rotl",
            "i32.rotr",
            "i32.eqz",
            "i32.eq",
            "i32.ne",
            "i32.lt_s",
            "i32.lt_u",
            "i32.gt_s",
            "i32.gt_u",
            "i32.le_s",
            "i32.le_u",
            "i32.ge_s",
            "i32.ge_u",

            "i64.clz",
            "i64.ctz",
            "i64.popcnt",
            "i64.add",
            "i64.sub",
            "i64.mul",
            "i64.div_s",
            "i64.div_u",
            "i64.rem_s",
            "i64.rem_u",
            "i64.and",
            "i64.or",
            "i64.xor",
            "i64.shl",
            "i64.shr_s",
            "i64.shr_u",
            "i64.rotl",
            "i64.rotr",
            "i64.eqz",
            "i64.eq",
            "i64.ne",
            "i64.lt_s",
            "i64.lt_u",
            "i64.gt_s",
            "i64.gt_u",
            "i64.le_s",
            "i64.le_u",
            "i64.ge_s",
            "i64.ge_u",

            "f32.abs",
            "f32.neg",
            "f32.ceil",
            "f32.floor",
            "f32.trunc",
            "f32.nearest",
            "f32.sqrt",
            "f32.add",
            "f32.sub",
            "f32.mul",
            "f32.div",
            "f32.min",
            "f32.max",
            "f32.copysign",
            "f32.eq",
            "f32.ne",
            "f32.lt",
            "f32.gt",
            "f32.le",
            "f32.ge",

            "f64.abs",
            "f64.neg",
            "f64.ceil",
            "f64.floor",
            "f64.trunc",
            "f64.nearest",
            "f64.sqrt",
            "f64.add",
            "f64.sub",
            "f64.mul",
            "f64.div",
            "f64.min",
            "f64.max",
            "f64.copysign",
            "f64.eq",
            "f64.ne",
            "f64.lt",
            "f64.gt",
            "f64.le",
            "f64.ge",

            "i32.wrap_i64",
            "i32.trunc_f32_s",
            "i32.trunc_f32_u",
            "i32.trunc_f64_s",
            "i32.trunc_f64_u",
            "i32.reinterpret_f32",

            "i64.extend_i32_s",
            "i64.extend_i32_u",
            "i64.trunc_f32_s",
            "i64.trunc_f32_u",
            "i64.trunc_f64_s",
            "i64.trunc_f64_u",
            "i64.reinterpret_f64",

            "f32.convert_i32_s",
            "f32.convert_i32_u",
            "f32.convert_i64_s",
            "f32.convert_i64_u",
            "f32.demote_f64",
            "f32.reinterpret_i32",

            "f64.convert_i32_s",
            "f64.convert_i32_u",
            "f64.convert_i64_s",
            "f64.convert_i64_u",
            "f64.promote_f32",
            "f64.reinterpret_i64",

            "i32.extend8_s",
            "i32.extend16_s",
            "i64.extend8_s",
            "i64.extend16_s",
            "i64.extend32_s",

            "i32.trunc_sat_f32_s",
            "i32.trunc_sat_f32_u",
            "i32.trunc_sat_f64_s",
            "i32.trunc_sat_f64_u",
            "i64.trunc_sat_f32_s",
            "i64.trunc_sat_f32_u",
            "i64.trunc_sat_f64_s",
            "i64.trunc_sat_f64_u",
        )
    }
}
