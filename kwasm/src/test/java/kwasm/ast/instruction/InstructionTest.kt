/*
 * Copyright 2021 Google LLC
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

package kwasm.ast.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.ast.type.Result
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InstructionTest {
    @Test
    fun list_flatten_whenEmpty() {
        val instructions = listOf<Instruction>()
        val flattened = instructions.flatten(0)
        assertThat(flattened).isEmpty()
    }

    @Test
    fun list_flatten_whenSinglePlain() {
        val instructions = listOf(NumericInstruction.F32AbsoluteValue)
        val flattened = instructions.flatten(0)
        assertThat(flattened).containsExactlyElementsIn(instructions).inOrder()
    }

    @Test
    fun list_flatten_whenMultiplePlain() {
        val instructions = listOf(
            NumericInstruction.F32AbsoluteValue,
            ControlInstruction.Return,
            ControlInstruction.Call(
                Index.ByIdentifier(Identifier.Function("foo"))
            )
        )
        val flattened = instructions.flatten(0)
        assertThat(flattened).containsExactlyElementsIn(instructions).inOrder()
    }

    @Test
    fun list_flatten_passesPosition_startingAtZero() {
        val instructions = (0..99).map { FakeInstruction() }
        val flattened = instructions.flatten(0)
        assertThat(flattened).hasSize(instructions.size)
        flattened.forEachIndexed { index, instruction ->
            instruction as FakeInstruction
            assertThat(instruction.passedIndex).isEqualTo(index)
        }
    }

    @Test
    fun list_flatten_passesPosition_startingAtNonZero() {
        val instructions = (0..99).map { FakeInstruction() }
        val flattened = instructions.flatten(1337)
        assertThat(flattened).hasSize(instructions.size)
        flattened.forEachIndexed { index, instruction ->
            instruction as FakeInstruction
            assertThat(instruction.passedIndex).isEqualTo(index + 1337)
        }
    }

    @Test
    fun baseInstruction_isPlain_defaultsToTrue() {
        assertThat(FakeInstruction().isPlain).isTrue()
    }

    @Test
    fun baseInstruction_flatten_defaultsToListOfSelf() {
        val instruction = FakeInstruction()
        assertThat(instruction.flatten(0)).containsExactlyElementsIn(listOf(instruction)).inOrder()
    }

    @Test
    fun blockInstruction_isPlain_defaultsToFalse() {
        assertThat(FakeBlockInstruction(ResultType(null)).isPlain).isFalse()
    }

    @Test
    fun markerInstruction_isPlain_defaultsToFalse() {
        assertThat(FakeMarkerInstruction().isPlain).isFalse()
    }

    @Test
    fun markerInstruction_flatten_defaultsToListOfSelf() {
        val instruction = FakeMarkerInstruction()
        assertThat(instruction.flatten(0)).containsExactlyElementsIn(listOf(instruction)).inOrder()
    }

    @Test
    fun markerInstruction_result_delegatesToOriginal() {
        val expectedResult = ResultType(Result(ValueType.I32))
        val instruction = FakeMarkerInstruction(expectedResult)
        assertThat(instruction.result).isEqualTo(expectedResult)
    }

    private class FakeInstruction : Instruction {
        var passedIndex: Int? = null
            private set

        override fun flatten(expressionIndex: Int): List<Instruction> {
            passedIndex = expressionIndex
            return listOf(this)
        }
    }

    private class FakeBlockInstruction(override val result: ResultType) : BlockInstruction {
        override fun flatten(expressionIndex: Int): List<Instruction> = listOf(this)
    }

    private class FakeMarkerInstruction(result: ResultType = ResultType(null)) : MarkerInstruction {
        override val identifier: Identifier.Label? = null
        override val original: BlockInstruction = FakeBlockInstruction(result)
    }
}
