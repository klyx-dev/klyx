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

package kwasm.validation.module

import kwasm.ast.instruction.Expression
import kwasm.ast.module.WasmFunction
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import kwasm.ast.util.MutableAstNodeIndex
import kwasm.validation.ModuleValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.instruction.validate
import kwasm.validation.validate
import kwasm.validation.validateNotNull

/** Validates the [WasmFunction] node. */
fun WasmFunction.validate(context: ValidationContext.Module): ValidationContext.Module =
    WasmFunctionValidator.visit(this, context)

/**
 * Validator of [WasmFunction] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#functions):
 *
 * * The type `C.types\[x]` must be defined in the context.
 * * Let `[t*^1] => [t?^2]` be the function type `C.types\[x]`.
 * * Let `C′` be the same context as `C`, but with:
 *     * `locals` set to the sequence of value types `t*^1 t*`, concatenating parameters and locals,
 *     * `labels` set to the singular sequence containing only result type `[t?^2]`.
 *     *  `return` set to the result type `[t?^2]`.
 * * Under the context `C′`, the expression `expr` must be valid with type `t?^2`.
 * * Then the function definition is valid with type `[t*^1] => [t?^2]`.
 */
object WasmFunctionValidator : ModuleValidationVisitor<WasmFunction> {
    override fun visit(
        node: WasmFunction,
        context: ValidationContext.Module
    ): ValidationContext.Module {
        val functionType = node.typeUse?.let { typeUse ->
            typeUse.index?.let {
                validateNotNull(
                    context.types[it],
                    parseContext = null,
                    message = "Expected type to be in module"
                ).functionType
            } ?: {
                validate(typeUse.toType() in context.types, parseContext = null) {
                    "Type $typeUse not found in module"
                }
                typeUse.functionType
            }()
        }

        val functionContext = context.toFunctionBody(
            locals = MutableAstNodeIndex<ValueType>().apply {
                functionType?.parameters?.forEach { this[it.id] = it.valType }
                node.locals.forEach { this[it.id] = it.valueType }
            },
            labels = MutableAstNodeIndex<ResultType>().apply {
                this[null] = ResultType(functionType?.returnValueEnums?.firstOrNull())
            },
            returnType = ResultType(functionType?.returnValueEnums?.firstOrNull())
        )

        // Validate the internals.
        Expression(node.instructions).validate(
            functionType?.returnValueEnums?.firstOrNull()?.valType,
            functionContext
        )

        return context
    }
}
