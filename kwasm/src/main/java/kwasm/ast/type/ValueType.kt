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

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/types.html#value-types):
 *
 * ```
 *   valtype ::=  { 'i32' -> I32
 *                  'i64' -> I64
 *                  'f32' -> F32
 *                  'f64' -> F64 }
 * ```
 */
sealed class ValueType : AstNode {
    object I32 : ValueType() {
        override fun toString(): String = "i32"
    }
    object I64 : ValueType() {
        override fun toString(): String = "i64"
    }
    object F32 : ValueType() {
        override fun toString(): String = "f32"
    }
    object F64 : ValueType() {
        override fun toString(): String = "f64"
    }
}
