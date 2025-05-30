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
import kwasm.ast.type.TableType

/**
 * Represents a table in a [WasmModule].
 *
 * From [the docs](https://webassembly.github.io/spec/core/syntax/modules.html#tables):
 *
 * The `tables` component of a module defines a vector of tables described by their table type:
 *
 * ```
 *   table ::= {type tabletype}
 * ```
 *
 * A table is a vector of opaque values of a particular table element type. The `min` size in the
 * limits of the table type specifies the initial size of that table, while its `max`, if present,
 * restricts the size to which it can grow later.
 *
 * Tables can be initialized through element segments.
 *
 * Tables are referenced through table indices, starting with the smallest index not referencing a
 * table import. Most constructs implicitly reference table index `0`.
 *
 * Note:** In the current version of WebAssembly, at most one table may be defined or imported in a
 * single module, and all constructs implicitly reference this table `0`. This restriction may be
 * lifted in future versions.
 */
data class Table(val id: Identifier.Table, val tableType: TableType) :
    AstNode
