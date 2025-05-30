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

package kwasm.validation.instruction.memory

import kwasm.ast.module.Index
import kwasm.validation.ValidationContext
import kwasm.validation.validateNotNull

/**
 * Validates that the receiving [ValidationContext] has a memory at index `0`.
 */
internal fun ValidationContext.validateMemoryExists() {
    validateNotNull(
        memories[Index.ByInt(0)],
        parseContext = null,
        message = "A memory must be included in the module (unknown memory)"
    )
}
