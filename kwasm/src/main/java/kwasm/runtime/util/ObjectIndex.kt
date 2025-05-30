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

package kwasm.runtime.util

import kwasm.ast.Identifier
import kwasm.ast.module.Index

/**
 * Index of arbitrary values of type [T]. Can be looked-up into by position, or by [Index].
 */
open class ObjectIndex<IdentifierType : Identifier, T>(
    private val contents: List<T> = emptyList()
) : MutableList<T> by contents.toMutableList() {
    private val itemIndexByIdentifier = mutableMapOf<Identifier, Int>()

    /** Adds the [value] to the [ObjectIndex], with accompanying (optional) [identifier]. */
    fun add(value: T, identifier: Identifier?) {
        identifier?.let { itemIndexByIdentifier[it] = size }
        add(value)
    }

    /**
     * Gets the value ([T]) from the [ObjectIndex] using the given [Index]. Returns `null` if it
     * can't be found.
     */
    operator fun get(index: Index<IdentifierType>): T? = when (index) {
        is Index.ByInt -> getOrNull(index.indexVal)
        is Index.ByIdentifier<IdentifierType> ->
            itemIndexByIdentifier[index.indexVal]?.let { getOrNull(it) }
    }

    /** Sets the value at the given [index] to a new [value]. */
    operator fun set(index: Index<*>, value: T) {
        when (index) {
            is Index.ByInt -> set(index.indexVal, value)
            is Index.ByIdentifier<*> ->
                itemIndexByIdentifier[index.indexVal]?.let { set(it, value) }
                    ?: add(value, index.indexVal)
        }
    }

    /** Returns whether or not the [ObjectIndex] contains a value for the given [index]. */
    operator fun contains(index: Index<*>): Boolean = when (index) {
        is Index.ByInt -> index.indexVal < size
        is Index.ByIdentifier<*> -> index.indexVal in itemIndexByIdentifier
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ObjectIndex<*, *>) return false

        if (contents != other.contents) return false
        if (itemIndexByIdentifier != other.itemIndexByIdentifier) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contents.hashCode()
        result = 31 * result + itemIndexByIdentifier.hashCode()
        return result
    }
}
