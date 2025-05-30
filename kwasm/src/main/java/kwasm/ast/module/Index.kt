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
 * Reference to an indexed [AstNode] in the AST.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#indices):
 *
 * ```
 *   typeidx(I)     ::= x:u32   => x
 *                      v:id    => x (if I.types[x] = v)
 *   funcidx(I)     ::= x:u32   => x
 *                      v:id    => x (if I.funcs[x] = v)
 *   tableidx(I)    ::= x:u32   => x
 *                      v:id    => x (if I.tables[x] = v)
 *   memidx(I)      ::= x:u32   => x
 *                      v:id    => x (if I.mems[x] = v)
 *   globalidx(I)   ::= x:u32   => x
 *                      v:id    => x (if I.globals[x] = v)
 *   lacalidx(I)    ::= x:u32   => x
 *                      v:id    => x (if I.locals[x] = v)
 *   labelidx(I)    ::= l:u32   => l
 *                      v:id    => l (if I.labels[l] = v)
 * ```
 */
sealed class Index<T : Identifier> : AstNode {
    data class ByInt(val indexVal: Int) : Index<Identifier>() {
        override fun toString(): String = indexVal.toString()
    }
    data class ByIdentifier<T : Identifier>(val indexVal: T) : Index<T>() {
        override fun toString(): String = indexVal.stringRepr ?: indexVal.unique.toString()
    }
}
