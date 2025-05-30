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

package kwasm.runtime.stack

import kwasm.ast.Identifier
import kwasm.runtime.StackElement

/**
 * Denotes a label value in an execution [Stack].
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#labels):
 *
 * Labels carry an argument arity `n` and their associated branch target, which is expressed
 * syntactically as an instruction sequence:
 *
 * ```
 *   label  ::= label_n{instr*}
 * ```
 *
 * Intuitively, `instr*` is the continuation to execute when the branch is taken, in place of the
 * original control construct.
 */
internal data class Label(
    val identifier: Identifier?,
    val arity: Int = 1,
    val opStackAtEnter: OperandStack,
    /**
     * Index within the current instruction list of the [kwasm.ast.instruction.BlockStart]
     * associated with the label.
     */
    val startPosition: Int,
    /**
     * Continuation position is the branch target start point for the current instruction list when
     * a break/jump is used within a block instruction.
     */
    val continuationPosition: Int
) : StackElement
