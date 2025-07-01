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

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#store):
 *
 * The store represents all global state that can be manipulated by WebAssembly programs. It
 * consists of the runtime representation of all instances of functions, tables, memories, and
 * globals that have been allocated during the life time of the abstract machine.
 *
 * Syntactically, the store is defined as a record listing the existing instances of each category:
 * ```
 *   store  ::= {
 *                  funcs funcinst*,
 *                  tables tableinst*,
 *                  mems meminst*,
 *                  globals globalinst*
 *              }
 * ```
 */
@ConsistentCopyVisibility
data class Store internal constructor(
    val functions: List<FunctionInstance> = emptyList(),
    val tables: List<Table> = emptyList(),
    val memories: List<Memory> = emptyList(),
    val globals: List<Global<*>> = emptyList()
) {
    /** Allocates the provided [FunctionInstance]. */
    fun allocateFunction(function: FunctionInstance): Allocation<Address.Function> =
        Allocation(copy(functions = functions + function), Address.Function(functions.size))

    /** Allocates the provided [Table]. */
    fun allocateTable(table: Table): Allocation<Address.Table> =
        Allocation(copy(tables = tables + table), Address.Table(tables.size))

    /** Allocates the provided [Memory]. */
    fun allocateMemory(memory: Memory): Allocation<Address.Memory> =
        Allocation(copy(memories = memories + memory), Address.Memory(memories.size))

    /** Allocates the provided [Global]. */
    fun allocateGlobal(global: Global<*>): Allocation<Address.Global> =
        Allocation(copy(globals = globals + global), Address.Global(globals.size))

    data class Allocation<T : Address>(val updatedStore: Store, val allocatedAddress: T)
}
