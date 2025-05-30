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
 * Represents a runtime exported value.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#export-instances):
 *
 * An export instance is the runtime representation of an export. It defines the export’s name and
 * the associated external value.
 *
 * ```
 *  exportinst ::= {name name, value externval}
 * ```
 */
sealed class Export(open val name: String, open val address: Address) {
    data class Function(
        override val name: String,
        override val address: Address.Function
    ) : Export(name, address)

    data class Table(
        override val name: String,
        override val address: Address.Table
    ) : Export(name, address)

    data class Memory(
        override val name: String,
        override val address: Address.Memory
    ) : Export(name, address)

    data class Global(
        override val name: String,
        override val address: Address.Global
    ) : Export(name, address)
}
