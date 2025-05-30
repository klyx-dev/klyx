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

import kwasm.runtime.Stack
import kwasm.runtime.StackElement
import java.util.LinkedList

/** Base implementation of [Stack]. */
internal abstract class BaseStack<T : StackElement>(
    private val name: String,
    initialValues: List<T> = emptyList(),
    val maxCapacity: Int = DEFAULT_MAX_CAPACITY
) : Stack<T> {
    val values = LinkedList<T>()

    init {
        require(initialValues.size <= maxCapacity) {
            "Initial values for stack: $name exceeds maximum capacity: $maxCapacity"
        }
        values.addAll(initialValues.asReversed())
    }

    override val height: Int
        get() = values.size

    override fun push(item: T) {
        check(height < maxCapacity) {
            "Stack overflow for stack: $name, exceeded max capacity: $maxCapacity"
        }
        values.push(item)
    }

    override fun pop(): T {
        check(height > 0) { "Stack: $name is empty" }
        return values.pop()
    }

    override fun peek(): T? = values.peek()

    override fun clear() = values.clear()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseStack<*>) return false

        if (name != other.name) return false
        if (maxCapacity != other.maxCapacity) return false
        if (!values.containsAll(other.values)) return false
        if (!other.values.containsAll(values)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + maxCapacity
        result = 31 * result + values.hashCode()
        return result
    }

    override fun toString(): String =
        "Stack(name='$name', maxCapacity=$maxCapacity, values=$values)"

    companion object {
        // TODO: tune?
        internal const val DEFAULT_MAX_CAPACITY = 4096
    }
}
