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

import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.runtime.ExecutionContext
import kwasm.runtime.toValue

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-const):
 *
 * 1. Push the value `t.const c` to the stack.
 */
internal fun NumericConstantInstruction<*>.execute(
    context: ExecutionContext
): ExecutionContext {
    when (this) {
        is NumericConstantInstruction.I32 ->
            context.stacks.operands.push(value.value.toValue())
        is NumericConstantInstruction.I64 ->
            context.stacks.operands.push(value.value.toValue())
        is NumericConstantInstruction.F32 ->
            context.stacks.operands.push(value.value.toValue())
        is NumericConstantInstruction.F64 ->
            context.stacks.operands.push(value.value.toValue())
    }
    context.instructionIndex++
    return context
}
