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

import kwasm.ast.module.DataSegment
import kwasm.ast.type.Result
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import kwasm.validation.ModuleValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.instruction.validateConstant
import kwasm.validation.validateNotNull

/** Validates the [DataSegment] node. */
fun DataSegment.validate(context: ValidationContext.Module): ValidationContext.Module =
    DataSegmentValidator.visit(this, context)

/**
 * Validator of [DataSegment] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#data-segments):
 *
 * * The memory `C.mems\[x]` must be defined in the context.
 * * The expression `expr` must be valid with result type `\[i32]`.
 * * The expression `expr` must be constant.
 * * Then the data segment is valid.
 */
object DataSegmentValidator : ModuleValidationVisitor<DataSegment> {
    override fun visit(
        node: DataSegment,
        context: ValidationContext.Module
    ): ValidationContext.Module {
        validateNotNull(context.memories[node.memoryIndex], parseContext = null) {
            "No memory found with index ${node.memoryIndex} in module " +
                "(unknown memory ${node.memoryIndex})"
        }

        node.offset.expression
            .validateConstant(
                ValueType.I32,
                context.toFunctionBody(returnType = ResultType(Result(ValueType.I32)))
            )

        return context
    }
}
