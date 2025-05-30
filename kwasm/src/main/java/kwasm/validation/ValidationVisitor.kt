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

package kwasm.validation

import kwasm.ast.AstNode
import kwasm.ast.instruction.Instruction

/**
 * Base visitor interface used during validation.
 *
 * [T] is the class of [AstNode] this [ValidationVisitor] is capable of validating, [Context] is
 * the required [ValidationContext] type needed to perform validation.
 */
interface ValidationVisitor<in T, Context> where T : AstNode, Context : ValidationContext {

    /**
     * Validates the given [node], and if necessary: its children (recursively, using other
     * [ValidationVisitor] instances).
     *
     * @throws ValidationException if the [node] or its children are found to be invalid.
     */
    fun visit(node: T, context: Context): Context
}

/**
 * Describes a [ValidationVisitor] intended for validating module-level [AstNode]s.
 * (e.g. [kwasm.ast.Table], [kwasm.ast.Memory], or [kwasm.ast.WasmFunction]).
 */
interface ModuleValidationVisitor<in T : AstNode> :
    ValidationVisitor<T, ValidationContext.Module>

/**
 * Describes a [ValidationVisitor] intended for validating parts of the body of a
 * [kwasm.ast.WasmFunction] or an [kwasm.ast.Expression].
 */
interface FunctionBodyValidationVisitor<in T : Instruction> :
    ValidationVisitor<T, ValidationContext.FunctionBody>
