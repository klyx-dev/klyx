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

package kwasm.ast.instruction

import kwasm.ast.AstNode
import kwasm.ast.Identifier
import kwasm.ast.type.ResultType

/** Base for all instruction [AstNode] implementations. */
interface Instruction : AstNode {
    val isPlain: Boolean
        get() = true

    /**
     * Flattens the instruction such that none of the [Instruction]s returned contain nested
     * instructions. May result in block-type instructions being converted to start/end variants,
     * wrapping their block contents.
     *
     * @param expressionIndex The index of this [Instruction] within a parent's flattened list. Used
     *  to configure start/end instructions and allow breaks/jumps to calculate instruction pointer
     *  offsets.
     */
    fun flatten(expressionIndex: Int): List<Instruction> = listOf(this)
}

/** Base for all instruction [AstNode] implementations which are "block" instructions. */
interface BlockInstruction : Instruction {
    val result: ResultType

    override val isPlain: Boolean
        get() = false

    override fun flatten(expressionIndex: Int): List<Instruction>
}

/** Represents a marker (either start or end) of a flattened [BlockInstruction]. */
interface MarkerInstruction : BlockInstruction {
    val identifier: Identifier.Label?
    val original: BlockInstruction

    override fun flatten(expressionIndex: Int): List<Instruction> = listOf(this)

    override val result: ResultType
        get() = original.result
}

/**
 * Marker of the start of a flattened [BlockInstruction]. The [endPosition] is used to know which
 * instruction should be jumped-to when leaving the [BlockInstruction].
 */
interface BlockStart : MarkerInstruction {
    val endPosition: Int
}

/**
 * Marker of the end of [BlockInstruction], during execution, the [startPosition] should be used to
 * locate the start of the block which is then used to locate the first instruction after the
 * original [BlockInstruction] (may not always be the next instruction after a [BlockEnd], for
 * example, the [BlockEnd]s used for each branch of an if instruction).
 */
interface BlockEnd : MarkerInstruction {
    val startPosition: Int
}

/** Defines an [AstNode] which represents an argument to an [Instruction]. */
interface Argument {
    /** Node which calculates the value of the argument. */
    val valueAstNode: AstNode
}

/**
 * Flattens the receiving [List] of [Instruction]s into a new list, keeping track of the
 * [startPosition] and appropriately passing it to the children, so that [BlockStart] and [BlockEnd]
 * instances can be created correctly.
 */
fun List<Instruction>.flatten(startPosition: Int): List<Instruction> {
    var current = startPosition
    val result = mutableListOf<Instruction>()
    forEach {
        val flattened = it.flatten(current)
        result.addAll(flattened)
        current += flattened.size
    }
    return result
}
