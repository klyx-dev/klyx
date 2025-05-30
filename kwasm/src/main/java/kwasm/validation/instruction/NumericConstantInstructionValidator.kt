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

import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.type.ValueType
import kwasm.validation.FunctionBodyValidationVisitor
import kwasm.validation.ValidationContext

/**
 * Validates [NumericConstantInstruction] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#valid-const):
 *
 * ```
 *   t.const c
 * ```
 *
 * * The instruction is valid with type `[] => \[t]`.
 */
object NumericConstantInstructionValidator :
    FunctionBodyValidationVisitor<NumericConstantInstruction<*>> {
    override fun visit(
        node: NumericConstantInstruction<*>,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        val outputType = when (node) {
            is NumericConstantInstruction.I32 -> ValueType.I32
            is NumericConstantInstruction.I64 -> ValueType.I64
            is NumericConstantInstruction.F32 -> ValueType.F32
            is NumericConstantInstruction.F64 -> ValueType.F64
        }
        return context.pushStack(outputType)
    }
}
