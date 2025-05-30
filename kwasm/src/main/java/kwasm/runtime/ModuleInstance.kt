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

import kwasm.api.MemoryProvider
import kwasm.ast.Identifier
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.WasmModule
import kwasm.runtime.FunctionInstance.Companion.allocate
import kwasm.runtime.util.AddressIndex
import kwasm.runtime.util.TypeIndex
import kwasm.validation.ValidationContext

/**
 * Represents an instantiated [kwasm.ast.module.WasmModule].
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#module-instances):
 *
 * A module instance is the runtime representation of a module. It is created by instantiating a
 * module, and collects runtime representations of all entities that are imported, defined, or
 * exported by the module.
 *
 * ```
 *   moduleinst ::= {
 *                      types functype*,
 *                      funcaddrs funcaddr*,
 *                      tableaddrs tableaddr*,
 *                      memaddrs memaddr*,
 *                      globaladdrs globaladdr*,
 *                      exports exportinst*
 *                  }
 * ```
 *
 * Each component references runtime instances corresponding to respective declarations from the
 * original module – whether imported or defined – in the order of their static indices. Function
 * instances, table instances, memory instances, and global instances are referenced with an
 * indirection through their respective addresses in the store.
 *
 * It is an invariant of the semantics that all export instances in a given module instance have
 * different names.
 */
data class ModuleInstance internal constructor(
    val types: TypeIndex,
    val functionAddresses: AddressIndex<Identifier.Function, Address.Function>,
    val tableAddresses: AddressIndex<Identifier.Table, Address.Table>,
    val memoryAddresses: AddressIndex<Identifier.Memory, Address.Memory>,
    val globalAddresses: AddressIndex<Identifier.Global, Address.Global>,
    val exports: List<Export>
) {
    /**
     * Returns a version of this [ModuleInstance] which only consists of the available globals.
     */
    fun toGlobalInitInstance(importedGlobals: List<Address.Global>): ModuleInstance {
        return ModuleInstance(
            TypeIndex(),
            AddressIndex(),
            AddressIndex(),
            AddressIndex(),
            AddressIndex(
                globalAddresses.filter { it in importedGlobals }
            ),
            emptyList()
        )
    }
}

/** Result of allocating a module using [WasmModule.allocate]. */
data class ModuleAllocationResult(
    val store: Store,
    val moduleInstance: ModuleInstance
)

/**
 * Instantiates a [WasmModule] into a [ModuleInstance] given a [Store].
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/modules.html#alloc-module):
 *
 * 1. Let `module` be the module to allocate and `externval*_im` the vector of external values
 *    providing the module’s imports, and `val*` the initialization values of the module’s globals.
 * 1. For each function `func_i` in `module.funcs`, do:
 *    * Let `funcaddr_i` be the function address resulting from allocating `func_i` for the module
 *      instance `moduleinst` defined below.
 * 1. For each table `table_i` in `module.tables`, do:
 *    * Let `tableaddr_i` be the table address resulting from allocating `table_i.type`.
 * 1. For each memory `mem_i` in `module.mems`, do:
 *    * Let `memaddr_i` be the memory address resulting from allocating `mem_i.type`.
 * 1. For each global `global_i` in `module.globals`, do:
 *    * Let `globaladdr_i` be the global address resulting from allocating `global_i.type` with
 *      initializer value `val*\[i]`.
 * 1. Let `funcaddr*` be the the concatenation of the function addresses `funcaddr_i`
 *    in index order.
 * 1. Let `tableaddr*` be the the concatenation of the table addresses `tableaddr_i`
 *    in index order.
 * 1. Let `memaddr*` be the the concatenation of the memory addresses `memaddr_i`
 *    in index order.
 * 1. Let `globaladdr*` be the the concatenation of the global addresses `globaladdr_i`
 *    in index order.
 * 1. Let `funcaddr*_mod` be the list of function addresses extracted from `externval*_im`,
 *    concatenated with `funcaddr*`.
 * 1. Let `tableaddr*_mod` be the list of table addresses extracted from `externval*_im`,
 *    concatenated with `tableaddr*`.
 * 1. Let `memaddr*_mod` be the list of memory addresses extracted from `externval*_im`,
 *    concatenated with `memaddr*`.
 * 1. Let `globaladdr*_mod` be the list of global addresses extracted from `externval*_im`,
 *    concatenated with `globaladdr*`.
 * 1. For each export `export_i` in `module.exports`, do:
 *    * If `export_i` is a function export for function index `x`, then let `externval_i` be the
 *      external value `func (funcaddr*_mod\[x])`.
 *    * Else, if `export_i` is a table export for table index `x`, then let `externval_i` be the
 *      external value `table (tableaddr*_mod\[x])`.
 *    * Else, if `export_i` is a memory export for memory index `x`, then let `externval_i` be the
 *      external value `mem (memaddr*_mod\[x])`.
 *    * Else, if `export_i` is a global export for global index `x`, then let `externval_i` be the
 *      external value `global (globaladdr*_mod\[x])`.
 *    * Let `exportinst_i` be the export instance `{name (export_i.name), value externval_i}`.
 * 1. Let `exportinst*` be the the concatenation of the export instances `exportinst_i` in index
 *    order.
 * 1. Let `moduleinst` be the module instance
 *    `{
 *        types (module.types),
 *        funcaddrs funcaddr*_mod,
 *        tableaddrs tableaddr*_mod,
 *        memaddrs memaddr*_mod,
 *        globaladdrs globaladdr*_mod,
 *        exports exportinst*
 *    }`.
 * 1. Return `moduleinst`.
 */
fun WasmModule.allocate(
    validationContext: ValidationContext.Module,
    memoryProvider: MemoryProvider,
    externalValues: List<ImportExtern<out Address>> = emptyList(),
    store: Store = Store()
): ModuleAllocationResult {
    var resultStore = store

    val funcAddrs = AddressIndex<Identifier.Function, Address.Function>()
    val tableAddrs = AddressIndex<Identifier.Table, Address.Table>()
    val memoryAddrs = AddressIndex<Identifier.Memory, Address.Memory>()
    val globalAddrs = AddressIndex<Identifier.Global, Address.Global>()
    val exportVals = mutableListOf<Export>()

    val externalFuncAddrs = externalValues.filterIsInstance<ImportExtern.Function>()
    val externalTableAddrs = externalValues.filterIsInstance<ImportExtern.Table>()
    val externalMemoryAddrs = externalValues.filterIsInstance<ImportExtern.Memory>()
    val externalGlobalAddrs = externalValues.filterIsInstance<ImportExtern.Global>()

    val moduleInstance = ModuleInstance(
        TypeIndex(types),
        funcAddrs,
        tableAddrs,
        memoryAddrs,
        globalAddrs,
        exportVals
    )

    externalFuncAddrs.forEach { funcAddrs.add(it.addressPlaceholder, it.identifier) }
    functions.forEach {
        val (newStore, addr) = resultStore.allocate(moduleInstance, it)
        funcAddrs.add(addr, it.id)
        resultStore = newStore
    }

    externalTableAddrs.forEach { tableAddrs.add(it.addressPlaceholder, it.identifier) }
    tables.forEach {
        val (newStore, addr) = resultStore.allocate(it.tableType)
        tableAddrs.add(addr, it.id)
        resultStore = newStore
    }

    externalMemoryAddrs.forEach { memoryAddrs.add(it.addressPlaceholder, it.identifier) }
    memories.forEach {
        val (newStore, addr) = resultStore.allocate(memoryProvider, it.memoryType)
        memoryAddrs.add(addr, it.id)
        resultStore = newStore
    }

    externalGlobalAddrs.forEach { globalAddrs.add(it.addressPlaceholder, it.identifier) }
    globals.forEach { global ->
        val (newStore, addr) =
            resultStore.allocate(global.globalType, global.globalType.valueType.zeroValue.value)
        globalAddrs.add(addr, global.id)
        resultStore = newStore
    }

    val exportValueAndIndex = mutableListOf<Pair<Export, Int>>()
    exports.forEach {
        val exportAndIndex = when (val descriptor = it.descriptor) {
            is ExportDescriptor.Function -> {
                val index = validationContext.functions.positionOf(descriptor.index)
                Export.Function(it.name, funcAddrs[index]) to index
            }
            is ExportDescriptor.Table -> {
                val index = validationContext.tables.positionOf(descriptor.index)
                Export.Table(it.name, tableAddrs[index]) to index
            }
            is ExportDescriptor.Memory -> {
                val index = validationContext.memories.positionOf(descriptor.index)
                Export.Memory(it.name, memoryAddrs[index]) to index
            }
            is ExportDescriptor.Global -> {
                val index = validationContext.globals.positionOf(descriptor.index)
                Export.Global(it.name, globalAddrs[index]) to index
            }
        }
        exportValueAndIndex.add(exportAndIndex)
    }
    exportVals.addAll(exportValueAndIndex.sortedBy { it.second }.map { it.first })

    return ModuleAllocationResult(resultStore, moduleInstance)
}
