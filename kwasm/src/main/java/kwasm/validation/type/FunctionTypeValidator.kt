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

package kwasm.validation.type

import kwasm.ast.type.FunctionType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationVisitor
import kwasm.validation.validate

/** Validates a [FunctionType] node. */
fun FunctionType.validate(context: ValidationContext) = FunctionTypeValidator.visit(this, context)

/**
 * Validator for a [FunctionType] node.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/types.html#function-types):
 *
 * Function types may not specify more than one result.
 *
 * ```
 * [tn^1] => [tm^2]
 * ```
 * * The arity `m` must not be larger than `1`.
 * * Then the function type is valid.
 */
object FunctionTypeValidator : ValidationVisitor<FunctionType, ValidationContext> {
    override fun visit(node: FunctionType, context: ValidationContext): ValidationContext {
        validate(node.returnValueEnums.size <= 1, parseContext = null) {
            "Function types may not specify more than one result"
        }
        return context
    }
}
