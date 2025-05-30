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

package kwasm.ast.module

import kwasm.ast.AstNode
import kwasm.ast.Identifier
import kwasm.ast.type.GlobalType
import kwasm.ast.type.MemoryType
import kwasm.ast.type.TableType

/**
 * Represents an import for a [WasmModule].
 *
 * From [the docs](https://webassembly.github.io/spec/core/syntax/modules.html#imports):
 *
 * The `imports` component of a module defines a set of imports that are required for instantiation.
 *
 * ```
 *   import     ::= {module name, name name, desc importdesc}
 *   importdesc ::= func typeidx
 *                  table tabletype
 *                  mem memtype
 *                  global globaltype
 * ```
 *
 * Each import is labeled by a two-level name space, consisting of a `module` name and a `name` for
 * an entity within that module. Importable definitions are functions, tables, memories, and
 * globals. Each import is specified by a descriptor with a respective type that a definition
 * provided during instantiation is required to match.
 *
 * Every import defines an index in the respective index space. In each index space, the indices of
 * imports go before the first index of any definition contained in the module itself.
 */
data class Import(
    val moduleName: String,
    val name: String,
    val descriptor: ImportDescriptor
) : AstNode

/** See [Import]. */
sealed class ImportDescriptor(open val id: Identifier) :
    AstNode {
    data class Function(
        override val id: Identifier.Function,
        val typeUse: TypeUse
    ) : ImportDescriptor(id)

    data class Table(
        override val id: Identifier.Table,
        val tableType: TableType
    ) : ImportDescriptor(id)

    data class Memory(
        override val id: Identifier.Memory,
        val memoryType: MemoryType
    ) : ImportDescriptor(id)

    data class Global(
        override val id: Identifier.Global,
        val globalType: GlobalType
    ) : ImportDescriptor(id)
}
