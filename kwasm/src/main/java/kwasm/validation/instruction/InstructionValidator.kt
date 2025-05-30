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

import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.instruction.Instruction
import kwasm.ast.instruction.MemoryInstruction
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.instruction.NumericInstruction
import kwasm.ast.instruction.ParametricInstruction
import kwasm.ast.instruction.VariableInstruction
import kwasm.util.Impossible
import kwasm.validation.FunctionBodyValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.instruction.control.ControlInstructionValidator

/** Validates the [Instruction]. */
fun Instruction.validate(context: ValidationContext.FunctionBody): ValidationContext.FunctionBody =
    InstructionValidator.visit(this, context)

/**
 * Validates [Instruction] nodes by multiplexing out to implementation-specific variants.
 */
object InstructionValidator : FunctionBodyValidationVisitor<Instruction> {
    override fun visit(
        node: Instruction,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody = when (node) {
        is NumericConstantInstruction<*> -> NumericConstantInstructionValidator.visit(node, context)
        is NumericInstruction -> NumericInstructionValidator.visit(node, context)
        is ParametricInstruction -> ParametricInstructionValidator.visit(node, context)
        is VariableInstruction -> VariableInstructionValidator.visit(node, context)
        is MemoryInstruction -> MemoryInstructionValidator.visit(node, context)
        is ControlInstruction -> ControlInstructionValidator.visit(node, context)
        else -> Impossible()
    }
}
