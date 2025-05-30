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

import kwasm.KWasmRuntimeException
import kwasm.ast.type.TableType

/**
 * Runtime representation of a WebAssembly Table.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#table-instances):
 *
 * A table instance is the runtime representation of a table. It holds a vector of function elements
 * and an optional maximum size, if one was specified in the table type at the table’s definition
 * site.
 *
 * Each function element is either empty, representing an uninitialized table entry, or a function
 * address. Function elements can be mutated through the execution of an element segment or by
 * external means provided by the embedder.
 *
 * ```
 *   tableinst ::= {elem vec(funcelem), max u32?}
 *   funcelem ::= funcaddr?
 * ```
 *
 * It is an invariant of the semantics that the length of the element vector never exceeds the
 * maximum size, if present.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
data class Table(
    private val mutableElements: MutableMap<Int, Address.Function?>,
    val maxSize: Int = UInt.MAX_VALUE.toInt()
) {
    val elements: Map<Int, Address.Function?>
        get() = mutableElements

    init {
        require(maxSize.toUInt() >= elements.size.toUInt()) {
            "Elements in table cannot exceed the maximum size: ${maxSize.toUInt()}"
        }
    }

    /**
     * Adds a function address to the Table.
     */
    fun addFunction(position: Int, address: Address.Function) {
        if (position >= elements.size) {
            throw KWasmRuntimeException(
                "Uninitialized slot for table at position $position (elements segment does not fit)"
            )
        }
        if (maxSize > -1 && position >= maxSize) {
            throw KWasmRuntimeException(
                "Elements in table cannot exceed the maximum size: ${maxSize.toUInt()} " +
                    "(elements segment does not fit)"
            )
        }
        mutableElements[position] = address
    }
}

/**
 * Builder to create a [Table] given a [TableType] and a [builder] function used to add elements to
 * the [Table]'s elements.
 */
@Suppress("EXPERIMENTAL_API_USAGE", "FunctionName")
fun Table(tableType: TableType, builder: (MutableMap<Int, Address.Function?>) -> Unit = {}): Table {
    val elements = mutableMapOf<Int, Address.Function?>().also {
        for (i in 0 until tableType.limits.min) {
            it[i.toInt()] = null
        }
    }.also(builder)
    return Table(elements, maxSize = tableType.limits.max?.toInt() ?: UInt.MAX_VALUE.toInt())
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/modules.html#alloc-table):
 *
 * 1. Let `tabletype` be the table type to allocate.
 * 1. Let `({min n, max m?} elemtype)` be the structure of table type `tabletype`.
 * 1. Let `a` be the first free table address in `S`.
 * 1. Let `tableinst` be the table instance `{elem(ϵ)^n, max m?}` with `n` empty elements.
 * 1. Append `tableinst` to the `tables` of `S`.
 * 1. Return `a`.
 */
fun Store.allocate(tableType: TableType): Store.Allocation<Address.Table> =
    allocateTable(Table(tableType))
