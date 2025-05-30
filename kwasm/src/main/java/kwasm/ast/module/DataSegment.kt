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
 * From [the docs](https://webassembly.github.io/spec/core/syntax/modules.html#data-segments):
 *
 * The initial contents of a memory are zero-valued bytes. The `data` component of a module defines
 * a vector of data segments that initialize a range of memory, at a given offset, with a static
 * vector of bytes.
 *
 * ```
 *   data ::= { data memidx, offset expr, init vec(byte)}
 * ```
 *
 * The `offset` is given by a constant expression.
 *
 * **Note:** In the current version of WebAssembly, at most one memory is allowed in a module.
 * Consequently, the only valid `memidx` is `0`.
 */
data class DataSegment(
    val memoryIndex: Index<Identifier.Memory>,
    val offset: Offset,
    val init: ByteArray
) : AstNode {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataSegment

        if (memoryIndex != other.memoryIndex) return false
        if (offset != other.offset) return false
        if (!init.contentEquals(other.init)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = memoryIndex.hashCode()
        result = 31 * result + offset.hashCode()
        result = 31 * result + init.contentHashCode()
        return result
    }
}
