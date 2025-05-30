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

package kwasm.ast.instruction

import kwasm.ast.Identifier
import kwasm.ast.module.Index

/**
 * Base for all variable [Instruction] implementations.
 *
 * See
 * [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#variable-instructions)
 * for more information.
 */
sealed class VariableInstruction : Instruction {

    /**
     * Represents the local.get instruction.
     */
    data class LocalGet(
        override val valueAstNode: Index<Identifier.Local>
    ) : VariableInstruction(), Argument

    /**
     * Represents the local.set instruction.
     */
    data class LocalSet(
        override val valueAstNode: Index<Identifier.Local>
    ) : VariableInstruction(), Argument

    /**
     * Represents the local.tee instruction.
     */
    data class LocalTee(
        override val valueAstNode: Index<Identifier.Local>
    ) : VariableInstruction(), Argument

    /**
     * Represents the global.get instruction.
     */
    data class GlobalGet(
        override val valueAstNode: Index<Identifier.Global>
    ) : VariableInstruction(), Argument

    /**
     * Represents the global.set instruction.
     */
    data class GlobalSet(
        override val valueAstNode: Index<Identifier.Global>
    ) : VariableInstruction(), Argument
}
