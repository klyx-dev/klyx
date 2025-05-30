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

package kwasm.runtime.stack

import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.runtime.ModuleInstance
import kwasm.runtime.StackElement
import kwasm.runtime.util.LocalIndex

/**
 * Represents an Activation Frame in a running WebAssembly program.
 *
 * From [the
 * docs](https://webassembly.github.io/spec/core/exec/runtime.html#activations-and-frames):
 *
 * Activation frames carry the return arity `n` of the respective function, hold the values of its
 * locals (including arguments) in the order corresponding to their static local indices, and a
 * reference to the function’s own module instance:
 *
 * ```
 *   activation ::= frame_n{frame}
 *   frame ::= {locals val*, module moduleinst}
 * ```
 *
 * The values of the locals are mutated by respective variable instructions.
 */
internal data class Activation(
    val functionIndex: Index<Identifier.Function>,
    val locals: LocalIndex,
    val module: ModuleInstance,
    val arity: Int = 1
) : StackElement
