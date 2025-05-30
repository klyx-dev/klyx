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
import kwasm.ast.instruction.MemArg
import kwasm.ast.instruction.MemoryInstruction
import kwasm.format.ParseContext
import kwasm.format.text.ParseResult
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MemoryInstructionTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("MemoryInstructionTest.wat")

    @Test
    fun instructionParsing_parsesMemoryInstructions() {
        val result = tokenizer.tokenize("i32.load", context).parseInstruction(0)
            ?: fail("expected an instruction")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(MemoryInstruction.LoadInt.I32_LOAD)
    }

    @Test
    fun parses_i32Load_withoutMemarg() {
        val result = parseMemoryInstruction("i32.load")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(32, 32, false, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I32_LOAD)
    }

    @Test
    fun parses_i32Load_withMemarg() {
        val result = parseMemoryInstruction("i32.load offset=0 align=4")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(32, 32, false, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I32_LOAD)
    }

    @Test
    fun parses_i64Load_withoutMemarg() {
        val result = parseMemoryInstruction("i64.load")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 64, false, MemArg.EIGHT)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD)
    }

    @Test
    fun parses_i64Load_withMemarg() {
        val result = parseMemoryInstruction("i64.load offset=0 align=8")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 64, false, MemArg.EIGHT)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD)
    }

    @Test
    fun parses_f32Load_withoutMemarg() {
        val result = parseMemoryInstruction("f32.load")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadFloat(32, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.LoadFloat.F32_LOAD)
    }

    @Test
    fun parses_f32Load_withMemarg() {
        val result = parseMemoryInstruction("f32.load offset=0 align=4")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadFloat(32, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.LoadFloat.F32_LOAD)
    }

    @Test
    fun parses_f64Load_withoutMemarg() {
        val result = parseMemoryInstruction("f64.load")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadFloat(64, MemArg.EIGHT)
        )
        assertThat(result.astNode === MemoryInstruction.LoadFloat.F64_LOAD)
    }

    @Test
    fun parses_f64Load_withMemarg() {
        val result = parseMemoryInstruction("f64.load offset=0 align=8")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadFloat(64, MemArg.EIGHT)
        )
        assertThat(result.astNode === MemoryInstruction.LoadFloat.F64_LOAD)
    }

    @Test
    fun parses_i32Load8_s_withoutMemarg() {
        val result = parseMemoryInstruction("i32.load8_s")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(32, 8, true, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I32_LOAD8_S)
    }

    @Test
    fun parses_i32Load8_s_withMemarg() {
        val result = parseMemoryInstruction("i32.load8_s offset=0 align=1")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(32, 8, true, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I32_LOAD8_S)
    }

    @Test
    fun parses_i32Load8_u_withoutMemarg() {
        val result = parseMemoryInstruction("i32.load8_u")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(32, 8, false, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I32_LOAD8_U)
    }

    @Test
    fun parses_i32Load8_u_withMemarg() {
        val result = parseMemoryInstruction("i32.load8_u offset=0 align=1")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(32, 8, false, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I32_LOAD8_U)
    }

    @Test
    fun parses_i32Load16_s_withoutMemarg() {
        val result = parseMemoryInstruction("i32.load16_s")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(32, 16, true, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I32_LOAD16_S)
    }

    @Test
    fun parses_i32Load16_s_withMemarg() {
        val result = parseMemoryInstruction("i32.load16_s offset=0 align=2")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(32, 16, true, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I32_LOAD16_S)
    }

    @Test
    fun parses_i32Load16_u_withoutMemarg() {
        val result = parseMemoryInstruction("i32.load16_u")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(32, 16, false, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I32_LOAD16_U)
    }

    @Test
    fun parses_i32Load16_u_withMemarg() {
        val result = parseMemoryInstruction("i32.load16_u offset=0 align=2")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(32, 16, false, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I32_LOAD16_U)
    }

    @Test
    fun parses_i64Load8_s_withoutMemarg() {
        val result = parseMemoryInstruction("i64.load8_s")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 8, true, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD8_S)
    }

    @Test
    fun parses_i64Load8_s_withMemarg() {
        val result = parseMemoryInstruction("i64.load8_s offset=0 align=1")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 8, true, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD8_S)
    }

    @Test
    fun parses_i64Load8_u_withoutMemarg() {
        val result = parseMemoryInstruction("i64.load8_u")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 8, false, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD8_U)
    }

    @Test
    fun parses_i64Load8_u_withMemarg() {
        val result = parseMemoryInstruction("i64.load8_u offset=0 align=1")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 8, false, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD8_U)
    }

    @Test
    fun parses_i64Load16_s_withoutMemarg() {
        val result = parseMemoryInstruction("i64.load16_s")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 16, true, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD16_S)
    }

    @Test
    fun parses_i64Load16_s_withMemarg() {
        val result = parseMemoryInstruction("i64.load16_s offset=0 align=2")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 16, true, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD16_S)
    }

    @Test
    fun parses_i64Load16_u_withoutMemarg() {
        val result = parseMemoryInstruction("i64.load16_u")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 16, false, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD16_U)
    }

    @Test
    fun parses_i64Load16_u_withMemarg() {
        val result = parseMemoryInstruction("i64.load16_u offset=0 align=2")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 16, false, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD16_U)
    }

    @Test
    fun parses_i64Load32_s_withoutMemarg() {
        val result = parseMemoryInstruction("i64.load32_s")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 32, true, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD32_S)
    }

    @Test
    fun parses_i64Load32_s_withMemarg() {
        val result = parseMemoryInstruction("i64.load32_s offset=0 align=4")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 32, true, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD32_S)
    }

    @Test
    fun parses_i64Load32_u_withoutMemarg() {
        val result = parseMemoryInstruction("i64.load32_u")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 32, false, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD32_U)
    }

    @Test
    fun parses_i64Load32_u_withMemarg() {
        val result = parseMemoryInstruction("i64.load32_u offset=0 align=4")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.LoadInt(64, 32, false, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.LoadInt.I64_LOAD32_U)
    }

    @Test
    fun parses_i32Store_withoutMemarg() {
        val result = parseMemoryInstruction("i32.store")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(32, 32, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I32_STORE)
    }

    @Test
    fun parses_i32Store_withMemarg() {
        val result = parseMemoryInstruction("i32.store offset=0 align=4")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(32, 32, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I32_STORE)
    }

    @Test
    fun parses_i32Store8_withoutMemarg() {
        val result = parseMemoryInstruction("i32.store8")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(32, 8, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I32_STORE8)
    }

    @Test
    fun parses_i32Store8_withMemarg() {
        val result = parseMemoryInstruction("i32.store8 offset=0 align=1")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(32, 8, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I32_STORE8)
    }

    @Test
    fun parses_i32Store16_withoutMemarg() {
        val result = parseMemoryInstruction("i32.store16")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(32, 16, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I32_STORE16)
    }

    @Test
    fun parses_i32Store16_withMemarg() {
        val result = parseMemoryInstruction("i32.store16 offset=0 align=2")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(32, 16, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I32_STORE16)
    }

    @Test
    fun parses_i64Store8_withoutMemarg() {
        val result = parseMemoryInstruction("i64.store8")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(64, 8, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I64_STORE8)
    }

    @Test
    fun parses_i64Store8_withMemarg() {
        val result = parseMemoryInstruction("i64.store8 offset=0 align=1")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(64, 8, MemArg.ONE)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I64_STORE8)
    }

    @Test
    fun parses_i64Store16_withoutMemarg() {
        val result = parseMemoryInstruction("i64.store16")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(64, 16, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I64_STORE16)
    }

    @Test
    fun parses_i64Store16_withMemarg() {
        val result = parseMemoryInstruction("i64.store16 offset=0 align=2")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(64, 16, MemArg.TWO)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I64_STORE16)
    }

    @Test
    fun parses_i64Store32_withoutMemarg() {
        val result = parseMemoryInstruction("i64.store32")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(64, 32, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I64_STORE32)
    }

    @Test
    fun parses_i64Store32_withMemarg() {
        val result = parseMemoryInstruction("i64.store32 offset=0 align=4")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreInt(64, 32, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.StoreInt.I64_STORE32)
    }

    @Test
    fun parses_f32Store_withoutMemarg() {
        val result = parseMemoryInstruction("f32.store")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreFloat(32, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.StoreFloat.F32_STORE)
    }

    @Test
    fun parses_f32Store_withMemarg() {
        val result = parseMemoryInstruction("f32.store offset=0 align=4")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreFloat(32, MemArg.FOUR)
        )
        assertThat(result.astNode === MemoryInstruction.StoreFloat.F32_STORE)
    }

    @Test
    fun parses_f64Store_withoutMemarg() {
        val result = parseMemoryInstruction("f64.store")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreFloat(64, MemArg.EIGHT)
        )
        assertThat(result.astNode === MemoryInstruction.StoreFloat.F64_STORE)
    }

    @Test
    fun parses_f64Store_withMemarg() {
        val result = parseMemoryInstruction("f64.store offset=0 align=8")

        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEqualTo(
            MemoryInstruction.StoreFloat(64, MemArg.EIGHT)
        )
        assertThat(result.astNode === MemoryInstruction.StoreFloat.F64_STORE)
    }

    @Test
    fun parses_size() {
        val result = parseMemoryInstruction("memory.size")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(MemoryInstruction.Size)
    }

    @Test
    fun parses_grow() {
        val result = parseMemoryInstruction("memory.grow")

        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(MemoryInstruction.Grow)
    }

    private fun parseMemoryInstruction(source: String): ParseResult<out MemoryInstruction> =
        tokenizer.tokenize(source, context).parseMemoryInstruction(0)
            ?: fail("expected an instruction")
}
