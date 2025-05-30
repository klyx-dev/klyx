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

package kwasm.ast.type

import kwasm.ast.AstNode
import kwasm.ast.Identifier

/**
 * Data class to hold a parameter's id and valuetype
 * from [the docs](https://webassembly.github.io/spec/core/text/types.html#function-types):
 *
 * ```
 *   param    ::=  ‘(’ ‘param’  id?  t:valtype ‘)’  => t
 * ```
 */
data class Param(val id: Identifier.Local, val valType: ValueType) : AstNode {
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Param) return false
        if (other.valType != valType) return false
        return other.id.stringRepr == id.stringRepr ||
            (other.id.stringRepr == null && id.stringRepr != null) ||
            (other.id.stringRepr != null && id.stringRepr == null)
    }

    override fun hashCode(): Int = valType.hashCode()
}
