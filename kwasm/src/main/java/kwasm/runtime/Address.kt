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

import kwasm.util.Impossible

/**
 * Addresses for [FunctionInstance]s, [Memory]s, [Table]s, and [Global]s.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#addresses):
 *
 * Function instances, table instances, memory instances, and global instances in the store are
 * referenced with abstract addresses. These are simply indices into the respective store component.
 *
 * ```
 *   addr       ::= 0|1|2|...
 *   funcaddr   ::= addr
 *   tableaddr  ::= addr
 *   memaddr    ::= addr
 *   globaladdr ::= addr
 * ```
 *
 * An embedder may assign identity to exported store objects corresponding to their addresses, even
 * where this identity is not observable from within WebAssembly code itself (such as for function
 * instances or immutable globals).
 */
sealed class Address(value: Int) {
    open var value: Int = value
        set(value) {
            require(field != -1) { "Cannot re-assign an address value" }
            field = value
        }

    /** Whether or not the address needs initialization. */
    fun needsInit(): Boolean = value == -1

    data class Function(override var value: Int) : Address(value)
    data class Table(override var value: Int) : Address(value)
    data class Memory(override var value: Int) : Address(value)
    data class Global(override var value: Int) : Address(value)

    companion object {
        /** Creates a placeholder [Address] of type [T]. */
        inline fun <reified T : Address> needingInit(): T = when (T::class) {
            Function::class -> Function(-1)
            Table::class -> Table(-1)
            Memory::class -> Memory(-1)
            Global::class -> Global(-1)
            else -> Impossible("Unsupported type: ${T::class}")
        } as T
    }
}
