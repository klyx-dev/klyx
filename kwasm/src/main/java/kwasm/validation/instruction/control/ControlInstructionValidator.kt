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

package kwasm.validation.instruction.control

import kwasm.ast.instruction.BlockInstruction
import kwasm.ast.instruction.ControlInstruction
import kwasm.validation.FunctionBodyValidationVisitor
import kwasm.validation.ValidationContext

/**
 * Validator of [ControlInstruction] nodes.
 *
 * See: [PlainValidator] and [BlockValidator] for more information.
 */
object ControlInstructionValidator : FunctionBodyValidationVisitor<ControlInstruction> {
    override fun visit(
        node: ControlInstruction,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody = when (node) {
        is BlockInstruction -> BlockValidator.visit(node, context)
        else -> PlainValidator.visit(node, context)
    }
}
