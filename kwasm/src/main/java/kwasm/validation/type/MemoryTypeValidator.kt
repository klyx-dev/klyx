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

import kwasm.ast.type.MemoryType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationVisitor
import kotlin.math.pow

/** Validates a [MemoryType] node. */
fun MemoryType.validate(context: ValidationContext) = MemoryTypeValidator.visit(this, context)

/**
 * Validator of [MemoryType] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/types.html#memory-types):
 *
 * * The limits `limits` must be valid within range `2^16`.
 * * Then the memory type is valid.
 */
object MemoryTypeValidator : ValidationVisitor<MemoryType, ValidationContext> {
    override fun visit(node: MemoryType, context: ValidationContext): ValidationContext {
        node.limits.validate(LIMITS_RANGE, context)
        return context
    }

    internal val LIMITS_RANGE = 2.0.pow(16).toLong()
}
