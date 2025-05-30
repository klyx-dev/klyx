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
 * Representation of an element segment for a table.
 *
 * From [the docs](https://webassembly.github.io/spec/core/syntax/modules.html#element-segments):
 *
 * The initial contents of a table is uninitialized. The `elem` component of a module defines a
 * vector of element segments that initialize a subrange of a table, at a given offset, from a
 * static vector of elements.
 *
 * ```
 *   elem ::= {table tableidx, offset expr, init vec(funcidx)}
 * ```
 *
 * The `offset` is given by a constant expression.
 *
 * **Note:** In the current version of WebAssembly, at most one table is allowed in a module.
 * Consequently, the only valid `tableidx` is `0`.
 */
data class ElementSegment(
    val tableIndex: Index<Identifier.Table>,
    val offset: Offset,
    val init: List<Index<Identifier.Function>>
) : AstNode
