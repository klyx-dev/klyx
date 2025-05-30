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

import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.WasmModule
import kwasm.ast.util.AstNodeIndex
import kwasm.validation.ModuleValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationException
import kwasm.validation.validate

/**
 * Validates a [WasmModule].
 *
 * This is the main entry point into the validation process.
 *
 * @throws ValidationException if the module, or any of its children, are deemed invalid.
 */
fun WasmModule.validate(context: ValidationContext.Module = ValidationContext(this)) =
    ModuleValidator.visit(this, context)

/**
 * Validator of [WasmModule]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#valid-module):
 *
 * Modules are classified by their mapping from the external types of their imports to those of
 * their exports.
 *
 * A module is entirely closed, that is, its components can only refer to definitions that appear
 * in the module itself. Consequently, no initial context is required. Instead, the context `C`
 * for validation of the module’s content is constructed from the definitions in the module.
 *
 * *KWasm Note:* The validation context will be passed to the [ModuleValidator]
 *
 * * Let `module` be the module to validate.
 * * Let `C` be a context where:
 *    * `C.types` is `module.types`,
 *    * `C.funcs` is `funcs(it*)` concatenated with `ft*`, with the import’s external types `it*`
 *      and the internal function types `ft*` as determined below,
 *    * `C.tables` is `tables(it*)` concatenated with `tt*`, with the import’s external types `it*`
 *      and the internal table types `tt*` as determined below,
 *    * `C.mems` is `mems(it*)` concatenated with `mt*`, with the import’s external types `it*` and
 *      the internal memory types `mt*` as determined below,
 *    * `C.globals` is `globals(it*)` concatenated with `gt*`, with the import’s external types
 *      `it*` and the internal global types `gt*` as determined below,
 *    * `C.locals` is empty,
 *    * `C.labels` is empty,
 *    * `C.return` is empty.
 * * Let `C′` be the context where `C′.globals` is the sequence `globals(it*)` and all other fields
 *   are empty.
 * * Under the context `C`:
 *   * For each `functype_i` in `module.types`, the function type `functype_i` must be valid.
 *   * For each `func_i` in `module.funcs`, the definition `func_i` must be valid with a function
 *      type `ft_i`.
 *   * For each `table_i` in `module.tables`, the definition `table_i` must be valid with a table
 *     type `tt_i`.
 *   * For each `mem_i` in `module.mems`, the definition `mem_i` must be valid with a memory type
 *     `mt_i`.
 *   * For each `global_i` in `module.globals:` Under the context `C′`, the definition `global_i`
 *     must be valid with a global type `gt_i`.
 *   * For each `elem_i` in `module.elem`, the segment `elem_i` must be valid.
 *   * For each `data_i` in `module.data`, the segment `data_i` must be valid.
 *   * If `module.start` is non-empty, then `module.start` must be valid.
 *   * For each `import_i` in `module.imports`, the segment `import_i` must be valid with an
 *      external type `it_i`.
 *   * For each `export_i` in `module.exports`, the segment `export_i` must be valid with external
 *      type `et_i`.
 * * The length of `C.tables` must not be larger than `1`.
 * * The length of `C.mems` must not be larger than `1`.
 * * All export names `export_i.name` must be different.
 * * Let `ft*` be the concatenation of the internal function types `ft_i`, in index order.
 * * Let `tt*` be the concatenation of the internal table types `tt_i`, in index order.
 * * Let `mt*` be the concatenation of the internal memory types `mt_i`, in index order.
 * * Let `gt*` be the concatenation of the internal global types `gt_i`, in index order.
 * * Let `it*` be the concatenation of external types `it_i` of the imports, in index order.
 * * Let `et*` be the concatenation of external types `et_i` of the exports, in index order.
 * * Then the module is valid with external types `it* => et*`.
 */
object ModuleValidator : ModuleValidationVisitor<WasmModule> {
    override fun visit(
        node: WasmModule,
        context: ValidationContext.Module
    ): ValidationContext.Module {
        val globalContext = context.copy(
            types = AstNodeIndex(),
            functions = AstNodeIndex(),
            tables = AstNodeIndex(),
            memories = AstNodeIndex()
        )
        node.globals.forEach { it.validate(globalContext) }

        var resultContext = node.memories.fold(context) { lastContext, memory ->
            memory.validate(lastContext)
        }
        resultContext = node.tables.fold(resultContext) { lastContext, table ->
            table.validate(lastContext)
        }
        resultContext = node.functions.fold(resultContext) { lastContext, function ->
            function.validate(lastContext)
        }
        resultContext = node.elements.fold(resultContext) { lastContext, element ->
            element.validate(lastContext)
        }
        resultContext = node.data.fold(resultContext) { lastContext, data ->
            data.validate(lastContext)
        }
        resultContext = node.start?.validate(resultContext) ?: resultContext
        resultContext = node.exports.fold(resultContext) { lastContext, export ->
            export.validate(lastContext)
        }
        resultContext = node.imports.fold(resultContext) { lastContext, import ->
            import.validate(lastContext)
        }
        val importedTables = node.imports.count { it.descriptor is ImportDescriptor.Table }
        val importedMemories = node.imports.count { it.descriptor is ImportDescriptor.Memory }

        validate(
            node.tables.size + importedTables <= 1,
            parseContext = null,
            message = "Modules are not allowed to include more than one table (multiple tables)"
        )
        validate(
            node.memories.size + importedMemories <= 1,
            parseContext = null,
            message = "Modules are not allowed to include more than one memory (multiple memories)"
        )
        val exportsByName = node.exports.groupBy { it.name }
        val duplicateExports = exportsByName.filter { it.value.size > 1 }
        validate(duplicateExports.isEmpty(), parseContext = null) {
            "The following export names are used more than once: ${duplicateExports.keys} " +
                "(duplicate export name)"
        }

        return resultContext
    }
}
