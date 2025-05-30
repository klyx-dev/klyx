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

import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.validation.ModuleValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.validateNotNull

/** Validates the [Export] node. */
fun Export.validate(context: ValidationContext.Module): ValidationContext.Module =
    ExportValidator.visit(this, context)

/** Validates the [ExportDescriptor] node. */
fun ExportDescriptor<*>.validate(context: ValidationContext.Module): ValidationContext.Module =
    ExportDescriptorValidator.visit(this, context)

/**
 * Validator of [Export] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#exports):
 *
 * * The export description `exportdesc` must be valid with external type `externtype`.
 * * Then the export is valid with external type `externtype`.
 */
object ExportValidator : ModuleValidationVisitor<Export> {
    override fun visit(node: Export, context: ValidationContext.Module): ValidationContext.Module =
        node.descriptor.validate(context)
}

/**
 * Validator of [ExportDescriptor] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#exports):
 *
 * ```
 *   func x
 * ```
 * * The function `C.funcs\[x]` must be defined in the context.
 * * Then the export description is valid with external type `func C.funcs\[x]`.
 *
 * ```
 *   table x
 * ```
 * * The table `C.tables\[x]` must be defined in the context.
 * * Then the export description is valid with external type `table C.tables\[x]`.
 *
 * ```
 *   mem x
 * ```
 * * The memory `C.mems\[x]` must be defined in the context.
 * * Then the export description is valid with external type `mem C.mems\[x]`.
 *
 * ```
 *   global x
 * ```
 * * The global `C.globals\[x]` must be defined in the context.
 * * Then the export description is valid with external type `global C.globals\[x]`.
 */
object ExportDescriptorValidator : ModuleValidationVisitor<ExportDescriptor<*>> {
    override fun visit(
        node: ExportDescriptor<*>,
        context: ValidationContext.Module
    ): ValidationContext.Module {
        when (node) {
            is ExportDescriptor.Function ->
                validateNotNull(
                    context.functions[node.index],
                    parseContext = null
                ) {
                    "Function with index ${node.index} not found in the module (unknown function)"
                }
            is ExportDescriptor.Table ->
                validateNotNull(
                    context.tables[node.index],
                    parseContext = null
                ) {
                    "Table with index ${node.index} not found in the module (unknown table)"
                }
            is ExportDescriptor.Memory ->
                validateNotNull(
                    context.memories[node.index],
                    parseContext = null
                ) {
                    "Memory with index ${node.index} not found in the module (unknown memory)"
                }
            is ExportDescriptor.Global ->
                validateNotNull(
                    context.globals[node.index],
                    parseContext = null
                ) {
                    "Global with index ${node.index} not found in the module (unknown global)"
                }
        }
        return context
    }
}
