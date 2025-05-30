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

package kwasm.runtime

/** Defines an element in one of the runtime [Stack]s. */
interface StackElement

/** Defines a runtime stack of [StackElement]s of type [T]. */
internal interface Stack<T : StackElement> {
    /** The height of the stack. */
    val height: Int

    /** Pushes the [item] onto the top of the [Stack]. */
    fun push(item: T)

    /** Pops the [StackElement] at the top of the [Stack] and returns it. */
    fun pop(): T

    /** Returns the [StackElement] at the top of the [Stack] without popping it. */
    fun peek(): T?

    /** Empties the [Stack]. */
    fun clear()
}

/**
 * Pops [StackElement]s off the [Stack] until the [predicate] returns `true`, and returns that
 * element or `null` if a value matching the predicate could not be found after popping the full
 * contents of the stack.
 */
internal inline fun <T : StackElement> Stack<T>.popUntil(predicate: (T) -> Boolean): T? {
    var top: T?
    do {
        top = peek()
    } while (top != null && !predicate(pop()))
    return top
}
