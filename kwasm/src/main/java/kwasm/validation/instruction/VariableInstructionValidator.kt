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

package kwasm.validation.instruction

import kwasm.ast.Identifier
import kwasm.ast.instruction.VariableInstruction
import kwasm.ast.module.Index
import kwasm.ast.type.GlobalType
import kwasm.ast.type.ValueType
import kwasm.validation.FunctionBodyValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.validate
import kwasm.validation.validateNotNull

/**
 * Validator of [VariableInstruction] nodes.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#variable-instructions):
 *
 * [VariableInstruction.LocalGet]
 * * The local `C.locals\[x]` must be defined in the context.
 * * Let `t` be the value type `C.locals\[x]`.
 * * Then the instruction is valid with type `[] => \[t]`.
 *
 * [VariableInstruction.LocalSet]
 * * The local `C.locals\[x]` must be defined in the context.
 * * Let `t` be the value type `C.locals\[x]`.
 * * Then the instruction is valid with type `\[t] => []`.
 *
 * [VariableInstruction.LocalTee]
 * * The local `C.locals\[x]` must be defined in the context.
 * * Let `t` be the value type `C.locals\[x]`.
 * * Then the instruction is valid with type `\[t] => \[t]`.
 *
 * [VariableInstruction.GlobalGet]
 * * The global `C.globals\[x]` must be defined in the context.
 * * Let `mut t` be the global type `C.globals\[x]`.
 * * Then the instruction is valid with type `[] => \[t]`.
 *
 * [VariableInstruction.GlobalSet]
 * * The global `C.globals\[x]` must be defined in the context.
 * * Let `mut t` be the global type `C.globals\[x]`.
 * * The mutability `mut` must be `var`.
 * * Then the instruction is valid with type `\[t] => []`.
 */
object VariableInstructionValidator : FunctionBodyValidationVisitor<VariableInstruction> {
    override fun visit(
        node: VariableInstruction,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody = when (node) {
        is VariableInstruction.LocalGet -> validateLocalGet(node, context)
        is VariableInstruction.LocalSet -> validateLocalSet(node, context)
        is VariableInstruction.LocalTee -> validateLocalTee(node, context)
        is VariableInstruction.GlobalGet -> validateGlobalGet(node, context)
        is VariableInstruction.GlobalSet -> validateGlobalSet(node, context)
    }

    private fun validateLocalGet(
        node: VariableInstruction.LocalGet,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        val localType = validateLocalExists(context, node.valueAstNode)
        return context.pushStack(localType)
    }

    private fun validateLocalSet(
        node: VariableInstruction.LocalSet,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        val localType = validateLocalExists(context, node.valueAstNode)
        val (top, updatedContext) = context.popStack()
        validate(
            top != null,
            parseContext = null,
            message = "local.set expects the stack to be non-empty"
        )
        validate(top == localType, parseContext = null) {
            "Local ${node.valueAstNode} has type $localType, but top of stack is $top"
        }
        return updatedContext
    }

    private fun validateLocalTee(
        node: VariableInstruction.LocalTee,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        val localType = validateLocalExists(context, node.valueAstNode)
        val (top, updatedContext) = context.popStack()
        validate(
            top != null,
            parseContext = null,
            message = "local.tee expects the stack to be non-empty"
        )
        validate(top == localType, parseContext = null) {
            "Local ${node.valueAstNode} has type $localType, but top of stack is $top"
        }
        return updatedContext.pushStack(localType)
    }

    private fun validateGlobalGet(
        node: VariableInstruction.GlobalGet,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        val globalType = validateGlobalExists(context, node.valueAstNode)
        return context.pushStack(globalType.valueType)
    }

    private fun validateGlobalSet(
        node: VariableInstruction.GlobalSet,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        val globalType = validateGlobalExists(context, node.valueAstNode)
        validate(
            globalType.mutable,
            parseContext = null,
            message = "global.set requires global to be mutable, but ${node.valueAstNode} is not"
        )

        val (top, updatedContext) = context.popStack()
        validate(
            top != null,
            parseContext = null,
            message = "global.set expects the stack to be non-empty"
        )
        validate(top == globalType.valueType, parseContext = null) {
            "Global ${node.valueAstNode} has type ${globalType.valueType}, but top of stack is $top"
        }
        return updatedContext
    }

    private fun validateLocalExists(
        context: ValidationContext.FunctionBody,
        index: Index<Identifier.Local>
    ): ValueType = validateNotNull(context.locals[index], parseContext = null) {
        "No local with index: $index is defined"
    }

    private fun validateGlobalExists(
        context: ValidationContext.FunctionBody,
        index: Index<Identifier.Global>
    ): GlobalType = validateNotNull(context.globals[index], parseContext = null) {
        "No global with index: $index is defined"
    }
}
