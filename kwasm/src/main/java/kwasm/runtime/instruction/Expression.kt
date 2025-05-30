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
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.flatten
import kwasm.runtime.ExecutionContext
import kwasm.runtime.stack.OperandStack

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#expressions):
 *
 * An expression is evaluated relative to a current frame pointing to its containing module
 * instance.
 *
 * 1. Jump to the start of the instruction sequence `instr*` of the expression.
 * 1. Execute the instruction sequence.
 * 1. Assert: due to validation, the top of the stack contains a value.
 * 1. Pop the value `val` from the stack.
 *
 * The value `val` is the result of the evaluation.
 */
internal fun Expression.execute(
    context: ExecutionContext
): ExecutionContext {
    val expressionContext = context.copy(
        stacks = context.stacks.copy(operands = OperandStack()),
        instructionIndex = 0
    )
    val flattened = instructions.flatten(0)
    val executionValue =
        flattened.executeFlattened(expressionContext).let {
            if (it.stacks.operands.height == 0) throw KWasmRuntimeException(
                "Expression expected to produce a value, but stack is empty"
            )
            it.stacks.operands.pop()
        }
    context.stacks.operands.push(executionValue)
    return context
}
