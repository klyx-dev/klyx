package com.klyx.ui.filetree

class NodeIdRegistry {
    private val idsByKey = mutableMapOf<Pair<String, String>, String>() // (contextId, path) -> id
    private val keysById = mutableMapOf<String, Pair<String, String>>() // id -> (contextId, path)
    private var counter = 0L

    /** contextId: the parent node's id when minting a child, or "root" when minting a root. */
    fun idFor(contextId: String, path: String): String {
        val key = contextId to path
        return idsByKey.getOrPut(key) {
            "node-${counter++}".also { keysById[it] = key }
        }
    }

    fun forget(id: String) {
        keysById.remove(id)?.let { idsByKey.remove(it) }
    }

    fun rename(id: String, newPath: String) {
        val existing = keysById[id] ?: return
        val contextId = existing.first
        idsByKey.remove(keysById[id])
        val newKey = contextId to newPath
        idsByKey[newKey] = id
        keysById[id] = newKey
    }

    fun move(id: String, newContextId: String, newPath: String) {
        keysById[id]?.let { idsByKey.remove(it) }
        val newKey = newContextId to newPath
        idsByKey[newKey] = id
        keysById[id] = newKey
    }
}
