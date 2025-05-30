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

import kwasm.ast.module.Global
import kwasm.validation.ModuleValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.instruction.validateConstant
import kwasm.validation.type.validate

/** Validates the [Global] node. */
fun Global.validate(context: ValidationContext.Module): ValidationContext.Module =
    GlobalValidator.visit(this, context)

/**
 * Validator of [Global] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#globals):
 *
 * * The global type `mut t` must be valid.
 * * The expression `expr` must be valid with result type `\[t]`.
 * * The expression `expr` must be constant.
 * * Then the global definition is valid with type `mut t`.
 */
object GlobalValidator : ModuleValidationVisitor<Global> {
    override fun visit(node: Global, context: ValidationContext.Module): ValidationContext.Module {
        node.globalType.validate(context)
        node.initExpression.validateConstant(node.globalType.valueType, context.toFunctionBody())
        return context
    }
}
