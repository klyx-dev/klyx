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

package kwasm.validation.instruction.control

import kwasm.ast.Identifier
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.module.Index
import kwasm.ast.type.ElementType
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import kwasm.util.Impossible
import kwasm.validation.FunctionBodyValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.instruction.validate
import kwasm.validation.validate
import kwasm.validation.validateNotNull

/**
 * Validator of "plain" (non-[kwasm.ast.instruction.BlockInstruction]) [ControlInstruction] nodes.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#control-instructions):
 *
 * ```
 *   nop
 * ```
 * * The instruction is valid with type `[] => []`.
 *
 * ```
 *   unreachable
 * ```
 * * The instruction is valid with type `[t*1] => [t*2]`, for any sequences of value types `t*1`
 *   and `t*2`.
 *
 * For the other types, see the `validate` extension functions on [ControlInstruction.Break],
 * [ControlInstruction.BreakIf], [ControlInstruction.BreakTable], [ControlInstruction.Return],
 * [ControlInstruction.Call], and [ControlInstruction.CallIndirect].
 */
object PlainValidator : FunctionBodyValidationVisitor<ControlInstruction> {
    override fun visit(
        node: ControlInstruction,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody = when (node) {
        is ControlInstruction.NoOp -> context
        ControlInstruction.Unreachable -> context
        is ControlInstruction.Break -> node.validateBreak(context)
        is ControlInstruction.BreakIf -> node.validateBreakIf(context)
        is ControlInstruction.BreakTable -> node.validateBreakTable(context)
        ControlInstruction.Return -> validateReturn(context)
        is ControlInstruction.Call -> node.validateCall(context)
        is ControlInstruction.CallIndirect -> node.validateCallIndirect(context)
        else -> Impossible()
    }
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#valid-br):
 *
 * ```
 *   br l
 * ```
 * * The label `C.labels\[l]` must be defined in the context.
 * * Let `[t?]` be the result type `C.labels\[l]`.
 * * Then the instruction is valid with type `[t*^1 t?] => [t*^2]`, for any sequences of value
 *   types `t*^1` and `t*^2`.
 */
internal fun ControlInstruction.Break.validateBreak(
    context: ValidationContext.FunctionBody
): ValidationContext.FunctionBody {
    val labelResult = context.validateLabelExists(labelIndex)

    labelResult.result?.valType?.let {
        val top = context.peekStack()
        validate(
            top == it,
            parseContext = null,
            message = "Expected $it at the top of the stack"
        )
    }
    return context
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#valid-br-if):
 *
 * ```
 *   br_if l
 * ```
 * * The label `C.labels\[l]` must be defined in the context.
 * * Let `[t?]` be the result type `C.labels\[l]`.
 * * Then the instruction is valid with type `[t? i32] => [t?]`.
 */
internal fun ControlInstruction.BreakIf.validateBreakIf(
    context: ValidationContext.FunctionBody
): ValidationContext.FunctionBody {
    val labelResult = context.validateLabelExists(labelIndex)

    val (expectedI32, updatedContext) = context.popStack()
    validate(
        expectedI32 == ValueType.I32,
        parseContext = null,
        message = "Expected i32 at the top of the stack"
    )

    return labelResult.result?.valType?.let {
        val (top, finalContext) = updatedContext.popStack()
        validate(
            top == it,
            parseContext = null,
            message = "Expected $it at the second position in the stack"
        )
        finalContext.pushStack(it)
    } ?: updatedContext
}

/**
 * From
 * [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#valid-br-table):
 *
 * ```
 *   br_table l* l_N
 * ```
 * * The label `C.labels\[l_N]` must be defined in the context.
 * * Let `[t?]` be the result type `C.labels\[l_N]`.
 * * For all `l_i` in `l*`, the label `C.labels\[l_i]` must be defined in the context.
 * * For all `l_i` in `l*`, `C.labels\[l_i]` must be `[t?]`.
 * * Then the instruction is valid with type `[t*^1 t? i32] => [t*^2]`, for any sequences of
 *   value types `t*^1` and `t*^2`.
 */
internal fun ControlInstruction.BreakTable.validateBreakTable(
    context: ValidationContext.FunctionBody
): ValidationContext.FunctionBody {
    val defaultResult = context.validateLabelExists(defaultTarget)
    targets.forEach {
        val resultType = context.validateLabelExists(it)
        validate(defaultResult == resultType, parseContext = null) {
            "Default label's result type is $defaultResult, but $it's is $resultType"
        }
    }

    val (expectedI32, updatedContext) = context.popStack()
    validate(
        expectedI32 == ValueType.I32,
        parseContext = null,
        message = "Expected i32 at the top of the stack"
    )

    return defaultResult.result?.valType?.let {
        val (top, finalContext) = updatedContext.popStack()
        validate(
            top == it,
            parseContext = null,
            message = "Expected $it at the second position in the stack"
        )
        finalContext
    } ?: updatedContext
}

/**
 * From
 * [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#valid-return):
 *
 * ```
 *   return
 * ```
 * * The return type `C.return` must not be absent in the context.
 * * Let `[t?]` be the result type of `C.return`.
 * * Then the instruction is valid with type `[t*^1 t?] => [t*^2]`, for any sequences of value
 *   types `t*^1` and `t*^2`.
 */
internal fun validateReturn(
    context: ValidationContext.FunctionBody
): ValidationContext.FunctionBody {
    val returnType = validateNotNull(
        context.returnType,
        parseContext = null,
        message = "Return type must not be absent"
    )

    return returnType.result?.valType?.let {
        val top = context.peekStack()
        validate(top == it, parseContext = null) {
            "Expected $it at the top of the stack (type mismatch)"
        }
        context
    } ?: context
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#valid-call):
 *
 * ```
 *   call x
 * ```
 * * The function `C.funcs\[x]` must be defined in the context.
 * * Then the instruction is valid with type `C.funcs\[x]`.
 */
internal fun ControlInstruction.Call.validateCall(
    context: ValidationContext.FunctionBody
): ValidationContext.FunctionBody {
    val function = validateNotNull(
        context.functions[functionIndex],
        parseContext = null,
        message = "Function with index: $functionIndex not found"
    )

    return context.validateStackForFunctionType(FunctionType(function.params, function.results))
}

/**
 * From [the
     * docs](https://webassembly.github.io/spec/core/valid/instructions.html#valid-call-indirect):
 *
 * ```
 *   call_indirect x
 * ```
 * * The table `C.tables[0]` must be defined in the context.
 * * Let `limits elemtype` be the table type `C.tables[0]`.
 * * The element type `elemtype` must be `funcref`.
 * * The type `C.types\[x]` must be defined in the context.
 * * Let `[t*^1] => [t*^2]` be the function type `C.types\[x]`.
 * * Then the instruction is valid with type `[t*^1 i32] => [t*^2]`.
 */
internal fun ControlInstruction.CallIndirect.validateCallIndirect(
    context: ValidationContext.FunctionBody
): ValidationContext.FunctionBody {
    val tableType = validateNotNull(
        context.tables[0],
        parseContext = null,
        message = "Expected table 0 to be defined"
    )
    validate(tableType.elemType == ElementType.FunctionReference, parseContext = null) {
        "Expected table type to be funcref, but was ${tableType.elemType}"
    }
    val functionType = typeUse.index?.let {
        validateNotNull(
            context.types[it]?.functionType,
            parseContext = null
        ) { "Type with index $it not found" }
    } ?: FunctionType(typeUse.params, typeUse.results).also { functionType ->
        validate(
            context.types.values.any { it?.functionType == functionType },
            parseContext = null
        ) { "Type with FunctionType $functionType not found" }
    }

    val (stackParams, poppedContext) = context.popStack(1)
    validate(stackParams.size == 1 && stackParams[0] == ValueType.I32) {
        "Expected i32 on top of stack for table position of call_indirect but " +
            "found ${context.stack}"
    }

    return poppedContext.validateStackForFunctionType(functionType)
}

internal fun ValidationContext.FunctionBody.validateLabelExists(
    index: Index<Identifier.Label>
): ResultType = validateNotNull(
    labels[index],
    parseContext = null,
    message = "Label with index: $index not found"
)

internal fun ValidationContext.FunctionBody.validateStackForFunctionType(
    function: FunctionType
): ValidationContext.FunctionBody {
    val (stackParams, poppedContext) = popStack(function.parameters.size)
    validate(stackParams.size == function.parameters.size, parseContext = null) {
        "Expected ${function.parameters.size} item(s) in the stack, but only found " +
            stackParams.size
    }
    validate(
        stackParams.withIndex().all { (index, paramType) ->
            function.parameters[index].valType == paramType
        },
        parseContext = null
    ) {
        "Expected ${function.parameters.map(Param::valType)} at the top of the stack, but " +
            "found $stackParams"
    }
    return function.returnValueEnums.fold(poppedContext) { pushed, result ->
        pushed.pushStack(result.valType)
    }
}
