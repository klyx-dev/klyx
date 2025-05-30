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

package kwasm.runtime.instruction

import kwasm.KWasmRuntimeException
import kwasm.ast.instruction.ParametricInstruction
import kwasm.runtime.ExecutionContext
import kwasm.runtime.IntValue

/**
 * Executes the receiving [ParametricInstruction].
 */
internal fun ParametricInstruction.execute(
    context: ExecutionContext
): ExecutionContext = when (this) {
    is ParametricInstruction.Drop -> this.execute(context)
    is ParametricInstruction.Select -> this.execute(context)
}.also { it.instructionIndex++ }

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-drop):
 *
 * ```
 *   drop
 * ```
 *
 * 1. Assert: due to validation, a value is on the top of the stack.
 * 1. Pop the value `val` from the stack.
 */
internal fun ParametricInstruction.Drop.execute(context: ExecutionContext): ExecutionContext {
    context.stacks.operands.pop()
    return context
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-select):
 *
 * ```
 *   select
 * ```
 *
 * 1. Assert: due to validation, a value of value type `i32` is on the top of the stack.
 * 1. Pop the value `i32.const c` from the stack.
 * 1. Assert: due to validation, two more values (of the same value type) are on the top of the
 *    stack.
 * 1. Pop the value `val_2` from the stack.
 * 1. Pop the value `val_1` from the stack.
 * 1. If `c` is not `0`, then:
 *    * Push the value `val_1` back to the stack.
 * 1. Else:
 *    * Push the value `val_2` back to the stack.
 */
internal fun ParametricInstruction.Select.execute(context: ExecutionContext): ExecutionContext {
    val testVal = context.stacks.operands.pop() as? IntValue
        ?: throw KWasmRuntimeException("Select expects i32 at the top of the stack.")
    val val2 = context.stacks.operands.pop()
    val val1 = context.stacks.operands.pop()

    if (val1::class != val2::class)
        throw KWasmRuntimeException("Select expects two values of equal type on the stack.")

    if (testVal.value == 0) {
        context.stacks.operands.push(val1)
    } else {
        context.stacks.operands.push(val2)
    }
    return context
}
