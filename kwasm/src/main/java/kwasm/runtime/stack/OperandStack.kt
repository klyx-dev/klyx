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

package kwasm.runtime.stack

import kwasm.runtime.Value

/** Holds the stack of operand [Value]s for use during execution. */
internal class OperandStack(
    initialValues: List<Value<*>> = emptyList(),
    maxCapacity: Int = DEFAULT_MAX_CAPACITY
) : BaseStack<Value<*>>(NAME, initialValues, maxCapacity) {
    /** Makes a copy of the stack. */
    fun copy(): OperandStack = OperandStack(values.toList(), maxCapacity)

    companion object {
        private const val NAME = "Op"
    }
}
