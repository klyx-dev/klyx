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

import kwasm.ast.type.TableType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationVisitor

/** Validates a [TableType] node. */
fun TableType.validate(context: ValidationContext) = TableTypeValidator.visit(this, context)

/**
 * Validator for [TableType] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/types.html#table-types):
 *
 * * The limits `limits` must be valid within range `2^32`.
 * * Then the table type is valid.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
object TableTypeValidator : ValidationVisitor<TableType, ValidationContext> {
    override fun visit(node: TableType, context: ValidationContext): ValidationContext {
        node.limits.validate(LIMITS_RANGE, context)
        return context
    }

    internal val LIMITS_RANGE = UInt.MAX_VALUE.toLong()
}
