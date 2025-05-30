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
import kwasm.ast.type.ResultType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class ControlInstructionTest {
    @Test
    fun block_flatten_empty() {
        val identifier = Identifier.Label("foo")
        val block = ControlInstruction.Block(identifier, ResultType(null), emptyList())

        val flattened = block.flatten(0)

        assertThat(flattened).hasSize(2)
        assertThat(flattened[0])
            .isEqualTo(ControlInstruction.StartBlock(identifier, block, 1))
        assertThat(flattened[1])
            .isEqualTo(ControlInstruction.EndBlock(identifier, block, 0))
    }

    @Test
    fun block_flatten_nonEmpty() {
        val identifier = Identifier.Label("foo")
        val blockBody = listOf(
            NumericInstruction.I32Add,
            NumericInstruction.F32Add,
            ControlInstruction.Break(Index.ByInt(0) as Index<Identifier.Label>)
        )
        val block = ControlInstruction.Block(identifier, ResultType(null), blockBody)

        val flattened = block.flatten(0)

        assertThat(flattened).hasSize(5)
        assertThat(flattened.first())
            .isEqualTo(ControlInstruction.StartBlock(identifier, block, 4))
        assertThat(flattened.subList(1, 1 + blockBody.size))
            .containsExactlyElementsIn(blockBody)
            .inOrder()
        assertThat(flattened.last())
            .isEqualTo(ControlInstruction.EndBlock(identifier, block, 0))
    }

    @Test
    fun block_flatten_nested() {
        val identifier = Identifier.Label("foo")
        val nestedIdentifier = Identifier.Label("bar")
        val nestedBlock = ControlInstruction.Block(nestedIdentifier, ResultType(null), emptyList())
        val blockBody = listOf(nestedBlock)
        val block = ControlInstruction.Block(identifier, ResultType(null), blockBody)

        val flattened = block.flatten(0)

        assertThat(flattened).hasSize(4)
        assertThat(flattened[0])
            .isEqualTo(ControlInstruction.StartBlock(identifier, block, 3))
        assertThat(flattened[1])
            .isEqualTo(ControlInstruction.StartBlock(nestedIdentifier, nestedBlock, 2))
        assertThat(flattened[2])
            .isEqualTo(ControlInstruction.EndBlock(nestedIdentifier, nestedBlock, 1))
        assertThat(flattened[3])
            .isEqualTo(ControlInstruction.EndBlock(identifier, block, 0))
    }

    @Test
    fun loop_flatten_empty() {
        val identifier = Identifier.Label("foo")
        val loop = ControlInstruction.Loop(identifier, ResultType(null), emptyList())

        val flattened = loop.flatten(0)

        assertThat(flattened).hasSize(2)
        assertThat(flattened[0])
            .isEqualTo(ControlInstruction.StartBlock(identifier, loop, 1))
        assertThat(flattened[1])
            .isEqualTo(ControlInstruction.EndBlock(identifier, loop, 0))
    }

    @Test
    fun loop_flatten_nonEmpty() {
        val identifier = Identifier.Label("foo")
        val blockBody = listOf(
            NumericInstruction.I32Add,
            NumericInstruction.F32Add,
            ControlInstruction.Break(Index.ByInt(0) as Index<Identifier.Label>)
        )
        val loop = ControlInstruction.Loop(identifier, ResultType(null), blockBody)

        val flattened = loop.flatten(0)

        assertThat(flattened).hasSize(5)
        assertThat(flattened.first())
            .isEqualTo(ControlInstruction.StartBlock(identifier, loop, 4))
        assertThat(flattened.subList(1, 1 + blockBody.size))
            .containsExactlyElementsIn(blockBody)
            .inOrder()
        assertThat(flattened.last())
            .isEqualTo(ControlInstruction.EndBlock(identifier, loop, 0))
    }

    @Test
    fun loop_flatten_nested() {
        val identifier = Identifier.Label("foo")
        val nestedIdentifier = Identifier.Label("bar")
        val nestedLoop = ControlInstruction.Loop(nestedIdentifier, ResultType(null), emptyList())
        val blockBody = listOf(nestedLoop)
        val loop = ControlInstruction.Loop(identifier, ResultType(null), blockBody)

        val flattened = loop.flatten(0)

        assertThat(flattened).hasSize(4)
        assertThat(flattened[0])
            .isEqualTo(ControlInstruction.StartBlock(identifier, loop, 3))
        assertThat(flattened[1])
            .isEqualTo(ControlInstruction.StartBlock(nestedIdentifier, nestedLoop, 2))
        assertThat(flattened[2])
            .isEqualTo(ControlInstruction.EndBlock(nestedIdentifier, nestedLoop, 1))
        assertThat(flattened[3])
            .isEqualTo(ControlInstruction.EndBlock(identifier, loop, 0))
    }

    @Test
    fun if_flatten_empty() {
        val identifier = Identifier.Label("foo")
        val block = ControlInstruction.If(identifier, ResultType(null), emptyList(), emptyList())

        val flattened = block.flatten(0)

        assertThat(flattened).hasSize(4)
        assertThat(flattened[0])
            .isEqualTo(ControlInstruction.StartIf(identifier, block, 1, 2, 3))
        assertThat(flattened[1])
            .isEqualTo(ControlInstruction.EndBlock(identifier, block, 0))
        assertThat(flattened[2])
            .isEqualTo(ControlInstruction.EndBlock(identifier, block, 0))
        assertThat(flattened[3])
            .isEqualTo(ControlInstruction.EndBlock(identifier, block, 0))
    }

    @Test
    fun if_flatten_nonEmpty() {
        val identifier = Identifier.Label("foo")
        val positives = listOf(
            NumericInstruction.F32Add,
            NumericInstruction.I32Add,
            NumericInstruction.F32Ceiling
        )
        val negatives = listOf(
            NumericInstruction.F64Add,
            NumericInstruction.I64Add,
            NumericInstruction.F64Ceiling
        )
        val block = ControlInstruction.If(identifier, ResultType(null), positives, negatives)

        val flattened = block.flatten(0)

        assertThat(flattened).hasSize(10)
        assertThat(flattened).containsExactly(
            ControlInstruction.StartIf(identifier, block, 1, 5, 9),
            NumericInstruction.F32Add,
            NumericInstruction.I32Add,
            NumericInstruction.F32Ceiling,
            ControlInstruction.EndBlock(identifier, block, 0),
            NumericInstruction.F64Add,
            NumericInstruction.I64Add,
            NumericInstruction.F64Ceiling,
            ControlInstruction.EndBlock(identifier, block, 0),
            ControlInstruction.EndBlock(identifier, block, 0),
        ).inOrder()
    }
}
