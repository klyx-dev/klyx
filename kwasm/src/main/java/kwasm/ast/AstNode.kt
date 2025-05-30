/*
 * Copyright 2019 Google LLC
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

package kwasm.ast

/** Base interface implemented by all members of the AST. */
interface AstNode

/** Represents an [AstNode] type for which there may be a canonical variant available. */
interface DeDupeableAstNode<T : AstNode> : AstNode {
    /**
     * Returns a canonical variant of the [AstNode], if one exists - to avoid unnecessary
     * objects on the heap.
     */
    fun deDupe(): T
}

/** A list of [AstNode]s. */
data class AstNodeList<T : AstNode>(private val members: List<T>) : List<T> by members, AstNode

/** Convenience function to build an [AstNodeList] in a similar manner to other list types. */
fun <T : AstNode> astNodeListOf(vararg values: T): AstNodeList<T> = AstNodeList(listOf(*values))
