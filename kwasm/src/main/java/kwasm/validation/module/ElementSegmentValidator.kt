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

package kwasm.validation.module

import kwasm.ast.module.ElementSegment
import kwasm.ast.type.ElementType
import kwasm.ast.type.Result
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import kwasm.validation.ModuleValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.instruction.validateConstant
import kwasm.validation.validate
import kwasm.validation.validateNotNull

/** Validates the [ElementSegment] node. */
fun ElementSegment.validate(context: ValidationContext.Module): ValidationContext.Module =
    ElementSegmentValidator.visit(this, context)

/**
 * Validator of [ElementSegment] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#element-segments):
 *
 * * The table `C.tables\[x]` must be defined in the context.
 * * Let `limits elemtype` be the table type `C.tables\[x]`.
 * * The element type `elemtype` must be `funcref`.
 * * The expression `expr` must be valid with result type `\[i32]`.
 * * The expression `expr` must be constant.
 * * For each `y_i` in `y*`, the function `C.funcs\[y]` must be defined in the context.
 * * Then the element segment is valid.
 */
object ElementSegmentValidator : ModuleValidationVisitor<ElementSegment> {
    override fun visit(
        node: ElementSegment,
        context: ValidationContext.Module
    ): ValidationContext.Module {
        val table = validateNotNull(context.tables[node.tableIndex], parseContext = null) {
            "Table with index ${node.tableIndex} not found in the module (unknown table)"
        }

        validate(table.elemType == ElementType.FunctionReference, parseContext = null) {
            "Table must have elemType = funcref, but was ${table.elemType}"
        }

        node.offset.expression
            .validateConstant(
                ValueType.I32,
                context.toFunctionBody(returnType = ResultType(Result(ValueType.I32)))
            )

        node.init.forEach {
            validateNotNull(context.functions[it], parseContext = null) {
                "Function with index $it not found in the module"
            }
        }

        return context
    }
}
