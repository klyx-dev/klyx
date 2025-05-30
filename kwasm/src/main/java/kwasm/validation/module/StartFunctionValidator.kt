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

import kwasm.ast.module.StartFunction
import kwasm.validation.ModuleValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.validate
import kwasm.validation.validateNotNull

/** Validates the [StartFunction] node. */
fun StartFunction.validate(context: ValidationContext.Module): ValidationContext.Module =
    StartFunctionValidator.visit(this, context)

/**
 * Validator of [StartFunction] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#start-function):
 *
 * * The function `C.funcs\[x]` must be defined in the context.
 * * The type of `C.funcs\[x]` must be `[] => []`.
 * * Then the start function is valid.
 */
object StartFunctionValidator : ModuleValidationVisitor<StartFunction> {
    override fun visit(
        node: StartFunction,
        context: ValidationContext.Module
    ): ValidationContext.Module {
        val func = validateNotNull(context.functions[node.funcIndex], parseContext = null) {
            "Function with index ${node.funcIndex} not found in the module (unknown function)"
        }

        validate(func.params.isEmpty() && func.results.isEmpty(), parseContext = null) {
            "Start function type expected to be [] => [], but is ${func.functionType} " +
                "(start function)"
        }

        return context
    }
}
