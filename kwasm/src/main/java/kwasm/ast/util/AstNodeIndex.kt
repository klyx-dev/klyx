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

package kwasm.ast.util

import kwasm.ast.AstNode
import kwasm.ast.Identifier
import kwasm.ast.module.Index

/**
 * Zero-indexed and named-lookup table of [AstNode]s.
 */
interface AstNodeIndex<T : AstNode> {
    val size: Int
    val values: Set<T?>

    operator fun get(identifier: Identifier): T?
    operator fun get(index: Index<*>): T?
    operator fun get(index: Int): T?
    operator fun contains(node: T): Boolean

    fun toMutableIndex(): MutableAstNodeIndex<T>
    fun toImmutableIndex(): AstNodeIndex<T>
    fun any(block: (T?) -> Boolean): Boolean

    fun positionOf(index: Index<*>): Int
}

/**
 * Mutable variant of [AstNodeIndex].
 *
 * **Note:** Not thread-safe.
 */
interface MutableAstNodeIndex<T : AstNode> : AstNodeIndex<T> {
    operator fun plusAssign(node: T)
    operator fun set(identifier: Identifier?, node: T)
    fun prepend(identifier: Identifier?, node: T): MutableAstNodeIndex<T>
    fun prepend(node: T): MutableAstNodeIndex<T>
}

/** Creates a new [AstNodeIndex]. */
fun <T : AstNode> AstNodeIndex(): AstNodeIndex<T> = AstNodeIndexImpl()

/** Creates a new [AstNodeIndex]. */
fun <T : AstNode> MutableAstNodeIndex(): MutableAstNodeIndex<T> = AstNodeIndexImpl()

@Suppress("EXPERIMENTAL_API_USAGE")
private data class AstNodeIndexImpl<T : AstNode>(
    private val nodes: MutableList<T?> = mutableListOf(),
    private val nodesByIdentifier: MutableMap<String, T> = mutableMapOf()
) : MutableAstNodeIndex<T> {

    override val size: Int
        get() = nodes.size

    override val values: Set<T?>
        get() = nodes.toSet()

    override operator fun plusAssign(node: T) {
        nodes += node
    }

    override operator fun set(identifier: Identifier?, node: T) {
        identifier?.stringRepr?.let {
            // TODO: throw an exception with the ParseContext when nodes have context.
            check(it !in nodesByIdentifier || nodesByIdentifier[it] == node) { "Identifier: \"$it\" is already in use" }
            nodesByIdentifier[it] = node
        }
        nodes += node
    }

    override fun prepend(identifier: Identifier?, node: T) = apply {
        nodes.add(0, node)
        identifier?.stringRepr?.let {
            // TODO: throw an exception with the ParseContext when nodes have context.
            check(it !in nodesByIdentifier) { "Identifier: \"$it\" is already in use" }
            nodesByIdentifier[it] = node
        }
    }

    override fun prepend(node: T) = apply {
        nodes.add(0, node)
    }

    override fun positionOf(index: Index<*>): Int = when (index) {
        is Index.ByIdentifier<*> -> {
            val node = requireNotNull(nodesByIdentifier[index.indexVal.stringRepr]) {
                "Node with index $index not found in module"
            }
            nodes.indexOf(node)
        }
        is Index.ByInt -> index.indexVal
    }

    override operator fun get(identifier: Identifier): T? =
        identifier.stringRepr?.let { nodesByIdentifier[it] }

    override operator fun get(index: Index<*>): T? = when (index) {
        is Index.ByIdentifier<*> -> get(index.indexVal)
        is Index.ByInt -> get(index.indexVal)
    }

    override operator fun get(index: Int): T? = nodes.getOrNull(index)

    override operator fun contains(node: T): Boolean = nodes.any { it == node }

    override fun toMutableIndex(): MutableAstNodeIndex<T> = AstNodeIndexImpl(
        mutableListOf<T?>().apply { addAll(nodes) },
        mutableMapOf<String, T>().apply { putAll(nodesByIdentifier) }
    )

    override fun toImmutableIndex(): AstNodeIndex<T> = AstNodeIndexImpl(
        mutableListOf<T?>().apply { addAll(nodes) },
        mutableMapOf<String, T>().apply { putAll(nodesByIdentifier) }
    )

    override fun any(block: (T?) -> Boolean): Boolean = nodes.any { block(it) }
}
