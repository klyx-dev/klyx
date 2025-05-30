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
import kwasm.ast.instruction.Expression
import kwasm.ast.type.GlobalType

/**
 * Represents a global in a [WasmModule].
 *
 * From [the docs](https://webassembly.github.io/spec/core/syntax/modules.html#globals):
 *
 * ```
 *   global ::= {type globaltype, init expr}
 * ```
 *
 * Each global stores a single value of the given global type. Its 𝗍𝗒𝗉𝖾 also specifies whether a
 * global is immutable or mutable. Moreover, each global is initialized with an 𝗂𝗇𝗂𝗍 value given by a
 * constant initializer expression.
 *
 * Globals are referenced through global indices, starting with the smallest index not referencing a
 * global import.
 */
data class Global(
    val id: Identifier.Global,
    val globalType: GlobalType,
    val initExpression: Expression
) : AstNode
