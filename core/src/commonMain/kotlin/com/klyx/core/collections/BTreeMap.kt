package com.klyx.core.collections

class BTreeMap<K : Comparable<K>, V>(numberOfEntriesInNode: Int = 12) {
    init {
        require(numberOfEntriesInNode % 2 == 0) {
            "Number of entries in a node must be even"
        }
    }

    private val rootNode = Node<K, V>(numberOfEntriesInNode)

    operator fun set(key: K, value: V) = put(key, value)

    fun put(key: K, value: V) {
        val entry = Entry(key, value)

        val putResponse = rootNode.put(entry)
        if (putResponse is Node.PutResponse.NodeFull) {
            rootNode.createNewTopLevel(putResponse)
        }
    }

    private fun Node<K, V>.createNewTopLevel(putResponse: Node.PutResponse.NodeFull<K, V>) {
        entries[0] = putResponse.promoted
        (1..entries.lastIndex).forEach { entries[it] = null }

        children[0] = putResponse.left
        children[1] = putResponse.right
        (2..children.lastIndex).forEach { children[it] = null }
    }

    override fun toString(): String = rootNode.toString(indentLevel = 0)

    data class Entry<K : Comparable<K>, V>(
        override val key: K,
        override val value: V
    ) : Map.Entry<K, V>, Comparable<Entry<K, V>> {
        override fun compareTo(other: Entry<K, V>): Int {
            return key.compareTo(other.key)
        }
    }
}

private class Node<K : Comparable<K>, V>(private val numberOfEntriesInNode: Int) {
    val entries = Array<BTreeMap.Entry<K, V>?>(numberOfEntriesInNode) { null }
    val children = Array<Node<K, V>?>(numberOfEntriesInNode + 1) { null }

    operator fun get(key: K): V? {
        return when (val location = getLocationOfValue(key)) {
            is LocationOfValue.Value -> entries[location.index]?.value
            is LocationOfValue.Child -> children[location.index]?.get(key)
        }
    }

    fun put(entry: BTreeMap.Entry<K, V>): PutResponse<K, V> {
        val hasChildren = hasChildren()

        return when {
            !hasChildren && entries.last() == null -> putInNodeWithEmptySpace(entry, null, null)
            !hasChildren && entries.last() != null -> splitAndPromoteLeaf(entry)
            hasChildren -> {
                when (val location = getLocationOfValue(entry.key)) {
                    is LocationOfValue.Value -> {
                        entries[location.index] = entry
                        PutResponse.Success
                    }

                    is LocationOfValue.Child -> {
                        val childNode = children[location.index]!!
                        return when (val putResponse = childNode.put(entry)) {
                            is PutResponse.Success -> putResponse
                            is PutResponse.NodeFull -> insertPromoted(putResponse)
                        }
                    }
                }
            }

            else -> throw IllegalStateException("Should be no unhandled cases")
        }
    }

    private fun splitAndPromoteLeaf(additionalEntry: BTreeMap.Entry<K, V>): PutResponse.NodeFull<K, V> {
        // TODO: optimize sort out
        val sorted = entries.filterNotNull().toTypedArray().apply { sort() }

        val middle = sorted[sorted.size / 2]
        val left = createNode { node ->
            (0 until sorted.size / 2).forEach {
                node.entries[it] = sorted[it]
            }
        }

        val right = createNode { node ->
            val offset = (numberOfEntriesInNode / 2) + 1
            (offset until sorted.size).forEach {
                node.entries[it - offset] = sorted[it]
            }
        }

        return PutResponse.NodeFull(middle, left, right)
    }

    private fun insertPromoted(putResponse: PutResponse.NodeFull<K, V>): PutResponse<K, V> {
        fun isFull() = (entries.indexOfFirst { it == null } == -1)

        return when (isFull()) {
            true -> {
                val halfNumEntry = numberOfEntriesInNode / 2
                when {
                    putResponse.promoted < entries[0]!! -> {
                        val oldRightSide = createNode { node ->
                            (halfNumEntry until numberOfEntriesInNode).forEachIndexed { index, oldPosition ->
                                node.entries[index] = entries[oldPosition]
                                node.children[index] = children[oldPosition]
                            }
                            node.children[halfNumEntry] = children[numberOfEntriesInNode]
                        }

                        val newLeftSide = createNode { node ->
                            node.entries[0] = putResponse.promoted
                            (1 until halfNumEntry).forEach {
                                node.entries[it] = entries[it - 1]
                            }

                            node.children[0] = putResponse.left
                            node.children[1] = putResponse.right

                            (1..halfNumEntry).forEach {
                                node.children[it + 1] = children[it]
                            }
                        }

                        PutResponse.NodeFull(
                            promoted = entries[halfNumEntry - 1]!!,
                            left = newLeftSide,
                            right = oldRightSide
                        )
                    }

                    putResponse.promoted > entries[0]!! && putResponse.promoted < entries.last()!! -> {
                        val indexOfPromoted = entries.indexOfFirst { it!!.key > putResponse.promoted.key }

                        val leftNode = createNode { node ->
                            (0 until indexOfPromoted).forEach {
                                node.entries[it] = entries[it]
                                node.children[it] = children[it]
                            }
                            node.children[indexOfPromoted] = putResponse.left
                        }

                        val rightNode = createNode { node ->
                            node.children[0] = putResponse.right
                            (indexOfPromoted until numberOfEntriesInNode).forEachIndexed { index, indexOld ->
                                node.entries[index] = entries[indexOld]
                                node.children[index + 1] = children[indexOld + 1]
                            }
                        }

                        return PutResponse.NodeFull(putResponse.promoted, leftNode, rightNode)
                    }

                    putResponse.promoted > entries[entries.lastIndex]!! -> {
                        for (entry in entries) {
                            require(entry != null) {
                                "entries should not be null"
                            }
                        }

                        val oldLeftSide = createNode { node ->
                            (0 until halfNumEntry).forEach {
                                node.entries[it] = entries[it]
                                node.children[it] = children[it]
                            }
                            node.children[halfNumEntry] = children[halfNumEntry]
                        }

                        val newRightSide = createNode { node ->
                            val offset = halfNumEntry + 1
                            (offset until numberOfEntriesInNode).forEach {
                                node.entries[it - offset] = entries[it]
                            }
                            node.entries[halfNumEntry - 1] = putResponse.promoted

                            val grabFromIndex = node.children.size / 2 + 1
                            (0..halfNumEntry - 2).forEach {
                                node.children[it] = children[grabFromIndex + it]
                            }
                            node.children[halfNumEntry - 1] = putResponse.left
                            node.children[halfNumEntry] = putResponse.right
                        }

                        PutResponse.NodeFull(
                            promoted = entries[halfNumEntry]!!,
                            left = oldLeftSide,
                            right = newRightSide
                        )
                    }

                    else -> throw IllegalStateException("should not happen")
                }
            }

            false -> putInNodeWithEmptySpace(putResponse.promoted, putResponse.left, putResponse.right)
        }
    }

    private fun putInNodeWithEmptySpace(
        newEntry: BTreeMap.Entry<K, V>,
        left: Node<K, V>?,
        right: Node<K, V>?
    ): PutResponse<K, V> {

        fun getIndexOfSpotForPut(newEntry: BTreeMap.Entry<K, V>): Int {
            require(entries.last() == null) { "last entry should be null" }
            entries.forEachIndexed { index, keyValue ->
                when {
                    keyValue == null -> return index
                    keyValue.key == newEntry.key -> return index
                    keyValue.key > newEntry.key -> return index
                }
            }

            throw IllegalStateException("should never get here")
        }

        val index = getIndexOfSpotForPut(newEntry)
        val existingEntry = entries[index]
        return when {
            existingEntry == null -> {
                entries[index] = newEntry
                children[index] = left
                children[index + 1] = right
                PutResponse.Success
            }

            existingEntry.key == newEntry.key -> {
                entries[index] = newEntry
                children[index] = left
                children[index + 1] = right
                PutResponse.Success
            }

            existingEntry.key > newEntry.key -> {
                (entries.size - 1 downTo index + 1).forEach {
                    if (entries[it - 1] == null) return@forEach
                    entries[it] = entries[it - 1]
                    children[it + 1] = children[it]
                }
                entries[index] = newEntry
                children[index] = left
                children[index + 1] = right
                PutResponse.Success
            }

            else -> throw IllegalStateException("should never get here")
        }
    }

    private fun hasChildren() = children.any { it != null }

    private fun getLocationOfValue(key: K): LocationOfValue {
        // TODO: can be optimized with a binary search or something since they are already ordered
        entries.forEachIndexed { index, keyValue ->
            when {
                keyValue == null -> return LocationOfValue.Child(index)
                keyValue.key == key -> return LocationOfValue.Value(index)
                keyValue.key > key -> return LocationOfValue.Child(index)
            }
        }

        return LocationOfValue.Child(children.lastIndex)
    }

    private fun createNode(config: (Node<K, V>) -> Unit): Node<K, V> {
        return Node<K, V>(numberOfEntriesInNode).also { config(it) }
    }

    fun toString(indentLevel: Int): String {
        val indent = (0..indentLevel).joinToString { "\t" }
        return (0 until numberOfEntriesInNode).fold("") { acc, next ->
            acc + (children[next]?.toString(indentLevel + 1)?.let { "\n$indent$it" } ?: "") +
                    (entries[next]?.let { "\n$indent$it" } ?: "")
        } + (children[numberOfEntriesInNode]?.toString(indentLevel + 1) ?: "")
    }

    sealed interface PutResponse<out K, out V> {
        object Success : PutResponse<Nothing, Nothing>
        data class NodeFull<K : Comparable<K>, V>(
            val promoted: BTreeMap.Entry<K, V>,
            val left: Node<K, V>,
            val right: Node<K, V>
        ) : PutResponse<K, V>
    }

    sealed interface LocationOfValue {
        data class Value(val index: Int) : LocationOfValue
        data class Child(val index: Int) : LocationOfValue
    }
}
