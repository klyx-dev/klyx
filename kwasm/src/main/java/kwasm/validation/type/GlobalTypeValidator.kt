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

import kwasm.ast.type.GlobalType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationVisitor

/** Validates a [GlobalType] node. */
fun GlobalType.validate(context: ValidationContext) = GlobalTypeValidator.visit(this, context)

/**
 * Validator of [GlobalType] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/types.html#global-types):
 *
 * * The global type is valid.
 */
object GlobalTypeValidator : ValidationVisitor<GlobalType, ValidationContext> {
    override fun visit(node: GlobalType, context: ValidationContext): ValidationContext {
        // No-op
        return context
    }
}
