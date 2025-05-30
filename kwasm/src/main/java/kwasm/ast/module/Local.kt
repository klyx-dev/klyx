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
import kwasm.ast.type.ValueType

/**
 * Represents a mutable local value for a [WasmFunction].
 *
 * From [the docs](https://webassembly.github.io/spec/core/syntax/modules.html#syntax-func):
 *
 * The `locals` declare a vector of mutable local variables and their types. These variables are
 * referenced through local indices in the function’s body. The index of the first local is the
 * smallest index not referencing a parameter.
 */
data class Local(val id: Identifier.Local?, val valueType: ValueType) : AstNode
