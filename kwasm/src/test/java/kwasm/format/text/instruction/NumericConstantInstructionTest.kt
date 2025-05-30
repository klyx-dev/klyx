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

package kwasm.format.text.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NumericConstantInstructionTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("NumericConstants.wat")

    @Test
    fun is_calledBy_parseInstruction() {
        val result = tokenizer.tokenize("i32.const 0x10", context)
            .parseInstruction(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.I32
        assertThat(instruction.value.value).isEqualTo(16)
    }

    @Test
    fun parses_i32Const() {
        val result = tokenizer.tokenize("i32.const 0x10", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.I32
        assertThat(instruction.value.value).isEqualTo(16)
    }

    @Test
    fun throws_if_i32Const_followedByFloat() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("i32.const 1.0", context)
                .parseNumericConstant(0)
        }
    }

    @Test
    fun parses_i64Const() {
        val result = tokenizer.tokenize("i64.const 0x100", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.I64
        assertThat(instruction.value.value).isEqualTo(256)
    }

    @Test
    fun throws_if_i64Const_followedByFloat() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("i64.const 1.0", context)
                .parseNumericConstant(0)
        }
    }

    @Test
    fun parses_f32Const() {
        val result = tokenizer.tokenize("f32.const -1.5e5", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.F32
        assertThat(instruction.value.value).isEqualTo(-1.5e5f)
    }

    @Test
    fun f32Const_followedByInt() {
        val result = tokenizer.tokenize("f32.const 1", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.F32
        assertThat(instruction.value.value).isEqualTo(1.0f)
    }

    @Test
    fun f32Const_followedByNan() {
        val result = tokenizer.tokenize("f32.const nan", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.F32
        assertThat(instruction.value.value).isEqualTo(Float.NaN)
    }

    @Test
    fun f32Const_followedByInf() {
        val result = tokenizer.tokenize("f32.const inf", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.F32
        assertThat(instruction.value.value).isEqualTo(Float.POSITIVE_INFINITY)
    }

    @Test
    fun f32Const_followedByNegativeInf() {
        val result = tokenizer.tokenize("f32.const -inf", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.F32
        assertThat(instruction.value.value).isEqualTo(Float.NEGATIVE_INFINITY)
    }

    @Test
    fun parses_f64Const() {
        val result = tokenizer.tokenize("f64.const -1.5e5", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.F64
        assertThat(instruction.value.value).isEqualTo(-1.5e5)
    }

    @Test
    fun f64Const_followedByInt() {
        val result = tokenizer.tokenize("f64.const 1", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.F64
        assertThat(instruction.value.value).isEqualTo(1.0)
    }

    @Test
    fun f64Const_followedByNan() {
        val result = tokenizer.tokenize("f64.const nan", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.F64
        assertThat(instruction.value.value).isEqualTo(Double.NaN)
    }

    @Test
    fun f64Const_followedByInf() {
        val result = tokenizer.tokenize("f64.const inf", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.F64
        assertThat(instruction.value.value).isEqualTo(Double.POSITIVE_INFINITY)
    }

    @Test
    fun f64Const_followedByNegativeInf() {
        val result = tokenizer.tokenize("f64.const -inf", context)
            .parseNumericConstant(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as NumericConstantInstruction.F64
        assertThat(instruction.value.value).isEqualTo(Double.NEGATIVE_INFINITY)
    }
}
