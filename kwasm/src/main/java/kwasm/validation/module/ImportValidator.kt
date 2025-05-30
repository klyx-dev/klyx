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

import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.validation.ModuleValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.type.validate
import kwasm.validation.validateNotNull

/** Validates the [Import] node. */
fun Import.validate(context: ValidationContext.Module): ValidationContext.Module =
    ImportValidator.visit(this, context)

/** Validates the [ImportDescriptor] node. */
fun ImportDescriptor.validate(context: ValidationContext.Module): ValidationContext.Module =
    ImportDescriptorValidator.visit(this, context)

/**
 * Validator of [Import] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#imports):
 *
 * * The import description `importdesc` must be valid with type `externtype`.
 * * Then the import is valid with type `externtype`.
 */
object ImportValidator : ModuleValidationVisitor<Import> {
    override fun visit(node: Import, context: ValidationContext.Module): ValidationContext.Module =
        node.descriptor.validate(context)
}

/**
 * Validator of [ImportDescriptor] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#imports):
 *
 * ```
 *   func x
 * ```
 * * The function `C.types\[x]` must be defined in the context.
 * * Let `[t*^1] => [t*^2]` be the function type `C.types\[x]`.
 * * Then the import description is valid with type `func [t*^1] => [t*^2]`.
 *
 * ```
 *   table tabletype
 * ```
 * * The table type `tabletype` must be valid.
 * * Then the import description is valid with type `table tabletype`.
 *
 * ```
 *   mem memtype
 * ```
 * * The memory type `memtype` must be valid.
 * * Then the import description is valid with type `mem memtype`.
 *
 * ```
 *   global globaltype
 * ```
 * * The global type `globaltype` must be valid.
 * * Then the import description is valid with type `global globaltype`.
 */
object ImportDescriptorValidator : ModuleValidationVisitor<ImportDescriptor> {
    override fun visit(
        node: ImportDescriptor,
        context: ValidationContext.Module
    ): ValidationContext.Module {
        when (node) {
            is ImportDescriptor.Function -> {
                node.typeUse.index?.let {
                    validateNotNull(context.types[it], parseContext = null) {
                        "Type with index $it not found in module"
                    }
                }
            }
            is ImportDescriptor.Table -> node.tableType.validate(context)
            is ImportDescriptor.Memory -> node.memoryType.validate(context)
            is ImportDescriptor.Global -> node.globalType.validate(context)
        }
        return context
    }
}
