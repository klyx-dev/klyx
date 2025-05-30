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

package kwasm.runtime

import kwasm.ast.Identifier
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.WasmModule

/**
 * Represents an external value imported by a [WasmModule]. Its address will be used to populate a
 * [ModuleInstance] so that lookups happen correctly at runtime.
 *
 * 1. Parse WasmModules
 * 1. Build ImportExterns from the modules' declared imports.
 * 1. Allocate the modules with their import externs.
 * 1. Using the allocations, update the addresses from the import externs appropriately.
 */
sealed class ImportExtern<T : Address>(
    open val moduleName: String,
    open val name: String,
    open val identifier: Identifier?,
    open val addressPlaceholder: T
) {
    data class Function(
        override val moduleName: String,
        override val name: String,
        override val identifier: Identifier.Function?,
        override val addressPlaceholder: Address.Function
    ) : ImportExtern<Address.Function>(moduleName, name, identifier, addressPlaceholder)

    data class Table(
        override val moduleName: String,
        override val name: String,
        override val identifier: Identifier.Table?,
        override val addressPlaceholder: Address.Table
    ) : ImportExtern<Address.Table>(moduleName, name, identifier, addressPlaceholder)

    data class Memory(
        override val moduleName: String,
        override val name: String,
        override val identifier: Identifier.Memory?,
        override val addressPlaceholder: Address.Memory
    ) : ImportExtern<Address.Memory>(moduleName, name, identifier, addressPlaceholder)

    data class Global(
        override val moduleName: String,
        override val name: String,
        override val identifier: Identifier.Global?,
        override val addressPlaceholder: Address.Global
    ) : ImportExtern<Address.Global>(moduleName, name, identifier, addressPlaceholder)
}

/** Transforms a [WasmModule]'s raw list of [Import]s into [ImportExtern]s. */
fun WasmModule.collectImportExterns(): List<ImportExtern<out Address>> = imports.map { import ->
    when (import.descriptor) {
        is ImportDescriptor.Function ->
            ImportExtern.Function(
                import.moduleName,
                import.name,
                import.descriptor.id.takeIf { it.stringRepr != null },
                Address.needingInit()
            )
        is ImportDescriptor.Table ->
            ImportExtern.Table(
                import.moduleName,
                import.name,
                import.descriptor.id.takeIf { it.stringRepr != null },
                Address.needingInit()
            )
        is ImportDescriptor.Memory ->
            ImportExtern.Memory(
                import.moduleName,
                import.name,
                import.descriptor.id.takeIf { it.stringRepr != null },
                Address.needingInit()
            )
        is ImportDescriptor.Global ->
            ImportExtern.Global(
                import.moduleName,
                import.name,
                import.descriptor.id.takeIf { it.stringRepr != null },
                Address.needingInit()
            )
    }
}
