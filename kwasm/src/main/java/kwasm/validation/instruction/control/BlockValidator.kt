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
import kwasm.ast.type.ValueType
import kwasm.util.Impossible
import kwasm.validation.FunctionBodyValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationException
import kwasm.validation.instruction.validate
import kwasm.validation.validate
import kwasm.validation.validateNotNull

/**
 * Validator of [kwasm.ast.instruction.BlockInstruction] [ControlInstruction] nodes.
 *
 * For specific validation requirements, see the docs for the `validate` extension functions on
 * [ControlInstruction.Block], [ControlInstruction.Loop], and [ControlInstruction.If].
 */
object BlockValidator : FunctionBodyValidationVisitor<ControlInstruction> {
    override fun visit(
        node: ControlInstruction,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody = when (node) {
        is ControlInstruction.Block -> node.validateBlock(context)
        is ControlInstruction.Loop -> node.validateLoop(context)
        is ControlInstruction.If -> node.validateIf(context)
        is ControlInstruction.StartBlock,
        is ControlInstruction.StartIf,
        is ControlInstruction.EndBlock ->
            throw ValidationException("Start/End markers not supported at validation time.")
        else -> Impossible()
    }
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#valid-block):
 *
 * ```
 *   block [t?] instr* end
 * ```
 * * Let `C′` be the same context as `C`, but with the result type `[t?]` prepended to the `labels`
 *   vector.
 * * Under context `C′`, the instruction sequence `instr*` must be valid with type `[] => [t?]`.
 * * Then the compound instruction is valid with type `[] => [t?]`.
 */
fun ControlInstruction.Block.validateBlock(
    context: ValidationContext.FunctionBody
): ValidationContext.FunctionBody {
    val innerContext = context.copy(
        labels = context.labels.toMutableIndex().prepend(label, result),
        stack = emptyList()
    )
    instructions.validate(
        innerContext,
        requiredEndStack = getRequiredEndStack(context),
        strictEndStackMatchRequired = true
    )
    return getRequiredEndStack(context).fold(context) { ctx, v -> ctx.pushStack(v) }
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#valid-block):
 *
 * ```
 *   block [t?] instr* end
 * ```
 * * Let `C′` be the same context as `C`, but with the result type `[t?]` prepended to the `labels`
 *   vector.
 * * Under context `C′`, the instruction sequence `instr*` must be valid with type `[] => [t?]`.
 * * Then the compound instruction is valid with type `[] => [t?]`.
 */
fun ControlInstruction.Loop.validateLoop(
    context: ValidationContext.FunctionBody
): ValidationContext.FunctionBody {
    val innerContext = context.copy(
        labels = context.labels.toMutableIndex().prepend(label, result),
        stack = emptyList()
    )
    instructions.validate(
        innerContext,
        requiredEndStack = getRequiredEndStack(context),
        strictEndStackMatchRequired = true
    )
    return getRequiredEndStack(context).fold(context) { ctx, v -> ctx.pushStack(v) }
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#valid-if):
 *
 * ```
 *   if [t?] instr*^1 else instr*^2 end
 * ```
 * * Let `C′` be the same context as `C`, but with the result type `[t?]` prepended to the `labels`
 *   vector.
 * * Under context `C′`, the instruction sequence `instr*^1` must be valid with type `[] => [t?]`.
 * * Under context `C′`, the instruction sequence `instr*^2` must be valid with type `[] => `[t?]`.
 * * Then the compound instruction is valid with type `\[i32] => `[t?]`.
 */
fun ControlInstruction.If.validateIf(
    context: ValidationContext.FunctionBody
): ValidationContext.FunctionBody {
    val (top, updatedContext) = context.popStack()
    validate(top == ValueType.I32, parseContext = null) {
        "Expected i32 at the top of the stack"
    }

    val innerContext = context.copy(
        labels = context.labels.toMutableIndex().prepend(label, result),
        stack = emptyList()
    )
    positiveInstructions.validate(
        innerContext,
        requiredEndStack = getRequiredEndStack(context),
        strictEndStackMatchRequired = true
    )
    if (negativeInstructions.isNotEmpty()) {
        negativeInstructions.validate(
            innerContext,
            requiredEndStack = getRequiredEndStack(context),
            strictEndStackMatchRequired = true
        )
    }
    return getRequiredEndStack(context).fold(updatedContext) { ctx, v -> ctx.pushStack(v) }
}

internal fun BlockInstruction.getRequiredEndStack(
    context: ValidationContext.FunctionBody
): List<ValueType> {
    val resultIndex = result.resultIndex
    return if (resultIndex == null) {
        result.result?.let { listOf(it.valType) } ?: emptyList()
    } else {
        val typeAtIndex = validateNotNull(
            context.types[resultIndex]?.functionType,
            parseContext = null
        ) { "Block type index specified, but none found at index: ${result.resultIndex}" }
        validate(typeAtIndex.parameters.isEmpty(), parseContext = null) {
            "Block type at specified index is not parameter-less: $typeAtIndex"
        }
        typeAtIndex.returnValueEnums.map { it.valType }
    }
}
