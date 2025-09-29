package com.klyx.editor.compose.text.buffer

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal class PieceTreeSearchCache(val limit: Int) {
    @Transient
    private val lock = reentrantLock()

    val cache = mutableListOf<CacheEntry>()

    operator fun get(offset: Int) = lock.withLock {
        cache.findLast { nodePos ->
            nodePos.nodeStartOffset <= offset && nodePos.nodeStartOffset + nodePos.node.piece.length >= offset
        }
    }

    fun getByLine(lineNumber: Int) = lock.withLock {
        cache.findLast { nodePos ->
            nodePos.nodeStartLineNumber in 1..<lineNumber && nodePos.nodeStartLineNumber + nodePos.node.piece.lineFeedCnt >= lineNumber
        }
    }

    fun insert(nodePosition: CacheEntry) = lock.withLock {
        if (cache.size >= limit) {
            cache.removeAt(0)
        }
        cache.add(nodePosition)
    }

    fun validate(offset: Int) = lock.withLock {
        val iter = cache.iterator()
        while (iter.hasNext()) {
            val nodePos = iter.next()
            if (nodePos.node.parent === NullTreeNode || nodePos.nodeStartOffset >= offset) {
                iter.remove()
            }
        }
    }
}
