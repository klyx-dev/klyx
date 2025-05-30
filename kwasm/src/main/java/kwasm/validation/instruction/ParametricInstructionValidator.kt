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

package kwasm.validation.instruction

import kwasm.ast.instruction.ParametricInstruction
import kwasm.ast.type.ValueType
import kwasm.validation.FunctionBodyValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.validate
import kwasm.validation.validateNotNull

/**
 * Validator of [ParametricInstruction] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#parametric-instructions):
 *
 * [ParametricInstruction.Drop]
 * * The instruction is valid with type `\[t] =>[]`, for any value type `t`.
 *
 * [ParametricInstruction.Select]
 * * The instruction is valid with type `[t t i32] => \[t]`, for any value type `t`.
 */
object ParametricInstructionValidator : FunctionBodyValidationVisitor<ParametricInstruction> {
    override fun visit(
        node: ParametricInstruction,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody = when (node) {
        is ParametricInstruction.Drop -> validateDrop(context)
        is ParametricInstruction.Select -> validateSelect(context)
    }

    private fun validateDrop(
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        val (type, updatedContext) = context.popStack()
        validateNotNull(type, parseContext = null, message = "Drop expects a non-empty stack")
        return updatedContext
    }

    private fun validateSelect(
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        val (types, updatedContext) = context.popStack(3)
        validate(
            types.size == 3,
            parseContext = null,
            message = "Select expects at least three ValueTypes on the stack"
        )
        validate(types[2] == ValueType.I32, parseContext = null) {
            "Select expects the ValueType at the top of the stack to be i32, " +
                "but ${types[2]} was found"
        }
        validate(types[0] == types[1], parseContext = null) {
            "Select expects the 2nd and 3rd ValueTypes to be the same, " +
                "but ${types[2]} != ${types[0]}"
        }
        return updatedContext.pushStack(types[1])
    }
}
