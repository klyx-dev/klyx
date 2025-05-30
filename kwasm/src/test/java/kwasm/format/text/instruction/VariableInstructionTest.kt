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
import kwasm.ast.instruction.VariableInstruction
import kwasm.format.ParseContext
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VariableInstructionTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("VariableConstants.wat")

    @Test
    fun is_calledBy_parseInstruction() {
        val result = tokenizer.tokenize("local.get \$var", context)
            .parseInstruction(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as VariableInstruction.LocalGet
        assertThat(instruction.valueAstNode.toString()).isEqualTo("\$var")
    }

    @Test
    fun parses_LocalGet() {
        val result = tokenizer.tokenize("local.get \$var", context)
            .parseVariableInstruction(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as VariableInstruction.LocalGet
        assertThat(instruction.valueAstNode.toString()).isEqualTo("\$var")
    }

    @Test
    fun parses_LocalSet() {
        val result = tokenizer.tokenize("local.set \$var", context)
            .parseVariableInstruction(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as VariableInstruction.LocalSet
        assertThat(instruction.valueAstNode.toString()).isEqualTo("\$var")
    }

    @Test
    fun parses_LocalTee() {
        val result = tokenizer.tokenize("local.tee \$var", context)
            .parseVariableInstruction(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as VariableInstruction.LocalTee
        assertThat(instruction.valueAstNode.toString()).isEqualTo("\$var")
    }

    @Test
    fun parses_GlobalGet() {
        val result = tokenizer.tokenize("global.get \$var", context)
            .parseVariableInstruction(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as VariableInstruction.GlobalGet
        assertThat(instruction.valueAstNode.toString()).isEqualTo("\$var")
    }

    @Test
    fun parses_GlobalSet() {
        val result = tokenizer.tokenize("global.set \$var", context)
            .parseVariableInstruction(0) ?: fail("Expected an instruction")
        assertThat(result.parseLength).isEqualTo(2)
        val instruction = result.astNode as VariableInstruction.GlobalSet
        assertThat(instruction.valueAstNode.toString()).isEqualTo("\$var")
    }
}
