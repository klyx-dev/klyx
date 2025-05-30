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

import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.Instruction
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.instruction.VariableInstruction
import kwasm.ast.type.ValueType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationVisitor
import kwasm.validation.validate

/** Validates a non-constant [Expression] node. */
fun Expression.validate(expectedResult: ValueType?, context: ValidationContext.FunctionBody) =
    ExpressionValidator(expectedResult).visit(this, context)

/** Validates a constant [Expression] node. */
fun Expression.validateConstant(
    expectedResult: ValueType?,
    context: ValidationContext.FunctionBody
) = ExpressionValidator(expectedResult, true).visit(this, context)

/**
 * Validator of [Expression] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#expressions):
 *
 * Expressions `expr` are classified by result types of the form `[t?]`.
 *
 * ```
 *   instr* end
 * ```
 * * The instruction sequence `instr*` must be valid with type `[] => [t?]`, for some optional
 *   value type `t?`.
 * * Then the expression is valid with result type `[t?]`.
 *
 * ### Constant Expressions
 *
 * * In a constant expression `instr* end` all instructions in `instr*` must be constant.
 * * A constant instruction `instr` must be:
 *    * either of the form `t.const c`,
 *    * or of the form `global.get x`, in which case `C.globals\[x]` must be a global type of the
 *      form `const t`.
 */
class ExpressionValidator(
    private val expectedResult: ValueType?,
    private val isConstant: Boolean = false
) : ValidationVisitor<Expression, ValidationContext.FunctionBody> {
    override fun visit(
        node: Expression,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        validate(
            !isConstant || node.instructions.all { it.isConstant(context) },
            parseContext = null,
            message = "Constant expressions may only contain ((i|f)(32|64)).const and " +
                "global.get instructions operating on non-mutable global values. " +
                "(constant expression required)"
        )

        return node.instructions.validate(
            context,
            requiredEndStack = expectedResult?.let { listOf(it) } ?: emptyList(),
            strictEndStackMatchRequired = true
        )
    }

    companion object {
        internal fun Instruction.isConstant(context: ValidationContext): Boolean {
            if (this is NumericConstantInstruction<*>) return true
            if (this !is VariableInstruction.GlobalGet) return false
            return context.globals[this.valueAstNode]?.mutable == false
        }
    }
}
