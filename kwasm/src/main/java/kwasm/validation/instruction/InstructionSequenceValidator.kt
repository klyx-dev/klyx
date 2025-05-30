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

import kwasm.ast.AstNodeList
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.instruction.Instruction
import kwasm.ast.type.ValueType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationVisitor
import kwasm.validation.instruction.control.validateReturn
import kwasm.validation.validate

/**
 * Validates a sequence of [Instruction]s.
 *
 * @param requiredEndStack see [InstructionSequenceValidator.requiredEndStack]
 * @param strictEndStackMatchRequired see [InstructionSequenceValidator.strictEndStackMatchRequired]
 */
fun List<Instruction>.validate(
    context: ValidationContext.FunctionBody,
    requiredEndStack: List<ValueType>? = null,
    strictEndStackMatchRequired: Boolean = false
) = InstructionSequenceValidator(
    requiredEndStack,
    strictEndStackMatchRequired
).visit(this as? AstNodeList<out Instruction> ?: AstNodeList(this), context)

/**
 * Validator of sequences of [Instruction] nodes.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#instruction-sequences):
 *
 * Typing of instruction sequences is defined recursively.
 *
 * Empty Instruction Sequence: `ϵ`
 * * The empty instruction sequence is valid with type `[t*] => [t*]`, for any sequence of value
 *   types `t*`.
 *
 * Non-empty Instruction Sequence: `instr* instrN`
 * * The instruction sequence `instr*` must be valid with type `[t*^1] => [t*2]`, for some sequences
 *   of value types `t*^1` and `t*^2`.
 * * The instruction `instrN` must be valid with type `[t*] => [t*^3]`, for some sequences of value
 *   types `t*` and `t*^3`.
 * * There must be a sequence of value types `t*^0`, such that `t*^2 = t*^0 t*`.
 * * Then the combined instruction sequence is valid with type `[t*^1] => [t*^0 t*^3]`.
 */
class InstructionSequenceValidator(
    /**
     * Represents the contents [ValidationContext.FunctionBody.stack] must contain the end of the
     * [Instruction] sequence, if required. `null` implies no required end-state.
     */
    private val requiredEndStack: List<ValueType>? = null,
    /**
     * When [requiredEndStack] is non-null, if this is `true`: the resultant
     * [ValidationContext.FunctionBody.stack] must match the [requiredEndStack] exactly. If `false`,
     * the contents of [requiredEndStack] must be the top-most elements in the resultant stack.
     */
    private val strictEndStackMatchRequired: Boolean = false
) : ValidationVisitor<AstNodeList<out Instruction>, ValidationContext.FunctionBody> {
    override fun visit(
        node: AstNodeList<out Instruction>,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        val resultContext = node.fold(context) { ctx, inst ->
            if (inst == ControlInstruction.Unreachable) return ctx
            if (inst == ControlInstruction.Return) return validateReturn(ctx)
            inst.validate(ctx)
        }
        requiredEndStack?.let {
            val topElements = resultContext.stack.takeLast(it.size)
            validate(topElements == it, parseContext = null) {
                "Required end stack is: $it, but instruction sequence results in: $topElements " +
                    "(type mismatch)"
            }
            validate(!strictEndStackMatchRequired || topElements.size == resultContext.stack.size) {
                "Strictly required end stack is: $it, but instruction sequence results in: " +
                    "${resultContext.stack}"
            }
        }
        return resultContext
    }
}
