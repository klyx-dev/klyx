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
import kwasm.ast.type.MemoryType

/**
 * Represents a memory instance in a [WasmModule].
 *
 * From [the docs](https://webassembly.github.io/spec/core/syntax/modules.html#memories):
 *
 * The `mems` component of a module defines a vector of linear memories (or memories for short) as
 * described by their memory type:
 *
 * ```
 *   mem    ::= {type memtype}
 * ```
 *
 * A memory is a vector of raw uninterpreted bytes. The `min` size in the limits of the memory type
 * specifies the initial size of that memory, while its `max`, if present, restricts the size to
 * which it can grow later. Both are in units of page size.
 *
 * Memories can be initialized through data segments.
 *
 * Memories are referenced through memory indices, starting with the smallest index not referencing
 * a memory import. Most constructs implicitly reference memory index `0`.
 *
 * **Note:** In the current version of WebAssembly, at most one memory may be defined or imported in
 * a single module, and all constructs implicitly reference this memory `0`. This restriction may
 * be lifted in future versions.
 */
data class Memory(
    val id: Identifier.Memory = Identifier.Memory(
        null,
        0
    ),
    val memoryType: MemoryType
) : AstNode
