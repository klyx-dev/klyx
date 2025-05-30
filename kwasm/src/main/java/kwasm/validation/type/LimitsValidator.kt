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

import kwasm.ast.type.Limits
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationVisitor
import kwasm.validation.validate

/** Validates a [Limits] node. */
fun Limits.validate(range: Long, context: ValidationContext) =
    LimitsValidator(range).visit(this, context)

/**
 * Validator of [Limits] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/types.html#limits):
 *
 * Limits must have meaningful bounds that are within a given range.
 *
 * ```
 * {min n, max m?}
 * ```
 *
 * * The value of `n` must not be larger than `k`.
 * * If the maximum `m?` is not empty, then:
 *    * Its value must not be larger than `k`.
 *    * Its value must not be smaller than `n`.
 * * Then the limit is valid within range `k`.
 */
class LimitsValidator(private val range: Long) : ValidationVisitor<Limits, ValidationContext> {
    override fun visit(node: Limits, context: ValidationContext): ValidationContext {
        validate(node.min <= range, parseContext = null) {
            "Limits' min value of ${node.min} must not be greater than $range (memory size " +
                "must be at most 65536 pages (4GiB))"
        }
        node.max?.let {
            validate(it <= range, parseContext = null) {
                "Limits' max value of $it must not be greater than $range (memory size must " +
                    "be at most 65536 pages (4GiB))"
            }
            validate(it >= node.min, parseContext = null) {
                "Limits' max value must be greater-than or equal to its " +
                    "min (min= ${node.min}, max= ${node.max}|size minimum must not be greater " +
                    "than maximum)"
            }
        }
        return context
    }
}
