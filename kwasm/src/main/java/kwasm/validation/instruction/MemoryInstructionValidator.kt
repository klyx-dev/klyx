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

import kwasm.ast.instruction.MemoryInstruction
import kwasm.validation.FunctionBodyValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.instruction.memory.GrowValidator
import kwasm.validation.instruction.memory.LoadFloatValidator
import kwasm.validation.instruction.memory.LoadIntValidator
import kwasm.validation.instruction.memory.SizeValidator
import kwasm.validation.instruction.memory.StoreFloatValidator
import kwasm.validation.instruction.memory.StoreIntValidator

/**
 * Validator of [MemoryInstruction] nodes by multiplexing into specific validators for each type.
 *
 * See: [LoadIntValidator], [StoreIntValidator], [LoadFloatValidator], [StoreFloatValidator],
 * [SizeValidator], [GrowValidator]
 */
object MemoryInstructionValidator : FunctionBodyValidationVisitor<MemoryInstruction> {
    override fun visit(
        node: MemoryInstruction,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody = when (node) {
        is MemoryInstruction.LoadInt -> LoadIntValidator.visit(node, context)
        is MemoryInstruction.StoreInt -> StoreIntValidator.visit(node, context)
        is MemoryInstruction.LoadFloat -> LoadFloatValidator.visit(node, context)
        is MemoryInstruction.StoreFloat -> StoreFloatValidator.visit(node, context)
        is MemoryInstruction.Size -> SizeValidator.visit(node, context)
        is MemoryInstruction.Grow -> GrowValidator.visit(node, context)
    }
}
