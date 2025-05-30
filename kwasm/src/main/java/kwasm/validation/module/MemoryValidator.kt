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

import kwasm.ast.module.Memory
import kwasm.validation.ModuleValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.type.validate

/** Validates the [Memory] node. */
fun Memory.validate(context: ValidationContext.Module): ValidationContext.Module =
    MemoryValidator.visit(this, context)

/**
 * Validator of [Memory] nodes.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/modules.html#memories):
 *
 * * The memory type `memtype` must be valid.
 * * Then the memory definition is valid with type `memtype`.
 */
object MemoryValidator : ModuleValidationVisitor<Memory> {
    override fun visit(node: Memory, context: ValidationContext.Module): ValidationContext.Module {
        node.memoryType.validate(context)
        return context
    }
}
