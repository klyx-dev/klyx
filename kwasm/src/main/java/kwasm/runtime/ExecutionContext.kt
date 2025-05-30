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

package kwasm.runtime

import kwasm.ast.instruction.Instruction
import kwasm.runtime.stack.RuntimeStacks
import kwasm.runtime.util.AddressIndex
import kwasm.runtime.util.TypeIndex

/** Current state of execution. Passed-to and returned-by all [Instruction]s. */
internal data class ExecutionContext(
    var store: Store,
    val moduleInstance: ModuleInstance,
    val stacks: RuntimeStacks,
    var instructionIndex: Int = 0,
    val flattenedInstructions: List<Instruction> = emptyList()
)

/** Creates an empty [ExecutionContext], useful when evaluating constant-[Expression]s. */
@Suppress("FunctionName")
internal fun EmptyExecutionContext(): ExecutionContext = ExecutionContext(
    Store(),
    ModuleInstance(
        TypeIndex(),
        AddressIndex(),
        AddressIndex(),
        AddressIndex(),
        AddressIndex(),
        emptyList()
    ),
    RuntimeStacks(),
    instructionIndex = 0,
    flattenedInstructions = emptyList()
)
