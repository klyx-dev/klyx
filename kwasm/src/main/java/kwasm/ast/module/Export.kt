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

/**
 * Defines an export for a [WasmModule].
 *
 * From [the docs](https://webassembly.github.io/spec/core/syntax/modules.html#exports):
 *
 * The `exports` component of a module defines a set of exports that become accessible to the host
 * environment once the module has been instantiated.
 *
 * ```
 *   export     ::= {name name, desc exportdesc}
 *   exportdesc ::= func funcidx
 *                  table tableidx
 *                  mem memidx
 *                  global globalidx
 * ```
 *
 * Each export is labeled by a unique name. Exportable definitions are functions, tables, memories,
 * and globals, which are referenced through a respective descriptor.
 */
data class Export(val name: String, val descriptor: ExportDescriptor<*>) :
    AstNode

/** See [Export]. */
sealed class ExportDescriptor<T : Identifier>(open val index: Index<T>) :
    AstNode {
    data class Function(
        override val index: Index<Identifier.Function>
    ) : ExportDescriptor<Identifier.Function>(index)

    data class Table(
        override val index: Index<Identifier.Table>
    ) : ExportDescriptor<Identifier.Table>(index)

    data class Memory(
        override val index: Index<Identifier.Memory>
    ) : ExportDescriptor<Identifier.Memory>(index)

    data class Global(
        override val index: Index<Identifier.Global>
    ) : ExportDescriptor<Identifier.Global>(index)
}
