package com.klyx.ui.filetree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Immutable
data class FileNode(
    val id: String,
    val name: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val childCount: Int? = null,
    val isPlaceholder: Boolean = false,
    /**
     * The project this node sits under, as `ProjectId.value`, or null when it is outside every project.
     */
    val projectId: String? = null,
)

enum class LoadState { LOADING, LOADED, ERROR }

@Immutable
data class VisibleFileNode(
    val node: FileNode,
    val depth: Int,
    val parentId: String?,
)

typealias ChildrenLoader = suspend (FileNode) -> List<FileNode>

@Stable
class FileTreeState(
    private val scope: CoroutineScope,
    private val childrenLoader: ChildrenLoader
) {
    val visibleNodes: SnapshotStateList<VisibleFileNode> = mutableStateListOf()

    private val expanded = mutableStateMapOf<String, Boolean>()
    private val loadState = mutableStateMapOf<String, LoadState>()

    private val childrenCache = HashMap<String, List<FileNode>>()
    private val pendingJobs = HashMap<String, Job>()

    var selectedId: String? by mutableStateOf(null)
        private set

    fun isExpanded(id: String): Boolean = expanded[id] == true
    fun isLoading(id: String): Boolean = loadState[id] == LoadState.LOADING

    fun select(id: String) {
        selectedId = id
    }

    /** Full reset: all roots replaced, everything else torn down. Prefer addRoot/removeNode for incremental changes. */
    fun setRoots(roots: List<FileNode>) {
        pendingJobs.values.forEach { it.cancel() }
        pendingJobs.clear()
        expanded.clear()
        loadState.clear()
        childrenCache.clear()
        visibleNodes.clear()
        visibleNodes.addAll(roots.map { VisibleFileNode(it, depth = 0, parentId = null) })
    }

    @IgnorableReturnValue
    fun addRoot(node: FileNode): VisibleFileNode {
        childrenCache.remove(node.id)
        val vn = VisibleFileNode(node, depth = 0, parentId = null)
        visibleNodes.add(vn)
        return vn
    }

    /** Adds [node] under [parentId]'s cached children, and inserts a visible row too if the parent is expanded. */
    fun insertChild(parentId: String, node: FileNode): VisibleFileNode? {
        childrenCache[parentId] = childrenCache[parentId].orEmpty() + node
        if (expanded[parentId] != true) return null

        val parentIndex = visibleNodes.indexOfFirst { it.node.id == parentId }
        if (parentIndex < 0) return null
        val parentDepth = visibleNodes[parentIndex].depth
        var insertAt = parentIndex + 1
        while (insertAt < visibleNodes.size && visibleNodes[insertAt].depth > parentDepth) insertAt++

        val vn = VisibleFileNode(node, parentDepth + 1, parentId)
        visibleNodes.add(insertAt, vn)
        return vn
    }

    /** Removes [id] and its whole subtree. Works for a root or any nested node, visible or cache-only. */
    fun removeNode(id: String) {
        val index = visibleNodes.indexOfFirst { vn -> vn.node.id == id }
        if (index < 0) {
            scrubFromCaches(id)
            return
        }

        val vn = visibleNodes[index]
        var end = index + 1
        while (end < visibleNodes.size && visibleNodes[end].depth > vn.depth) end++

        for (i in index until end) {
            val nodeId = visibleNodes[i].node.id
            pendingJobs.remove(nodeId)?.cancel()
            loadState.remove(nodeId)
            childrenCache.remove(nodeId)
            expanded.remove(nodeId)
        }
        visibleNodes.subList(index, end).clear()

        vn.parentId?.let { parentId ->
            childrenCache[parentId]?.let { childrenCache[parentId] = it.filterNot { c -> c.id == id } }
        }
    }

    private fun scrubFromCaches(id: String) {
        for ((val parentId = key, val children = value) in childrenCache) {
            if (children.any { child -> child.id == id }) {
                childrenCache[parentId] = children.filterNot { child -> child.id == id }
                return
            }
        }
    }

    fun renameNode(id: String, newName: String, newAbsolutePath: String) {
        val index = visibleNodes.indexOfFirst { it.node.id == id }
        if (index < 0) {
            renameInCaches(id, newName, newAbsolutePath)
            return
        }

        val vn = visibleNodes[index]
        val updated = vn.node.copy(name = newName, absolutePath = newAbsolutePath)
        visibleNodes[index] = vn.copy(node = updated)
        vn.parentId?.let { parentId ->
            childrenCache[parentId]?.let { childrenCache[parentId] = it.map { c -> if (c.id == id) updated else c } }
        }
    }

    private fun renameInCaches(id: String, newName: String, newAbsolutePath: String) {
        for (entry in childrenCache) {
            val parentId = entry.key
            val children = entry.value
            if (children.any { child -> child.id == id }) {
                childrenCache[parentId] =
                    children.map { child ->
                        if (child.id == id) child.copy(
                            name = newName,
                            absolutePath = newAbsolutePath
                        ) else child
                    }
                return
            }
        }
    }

    /** Relocates [id] under [newParentId] (null = new root), preserving its own expansion/cache state and its subtree. */
    fun moveNode(id: String, newParentId: String?, newAbsolutePath: String) {
        val index = visibleNodes.indexOfFirst { vn -> vn.node.id == id }
        val movedNode: FileNode

        if (index >= 0) {
            val vn = visibleNodes[index]
            movedNode = vn.node.copy(absolutePath = newAbsolutePath)
            var end = index + 1
            while (end < visibleNodes.size && visibleNodes[end].depth > vn.depth) end++
            visibleNodes.subList(index, end).clear()
            val parent = vn.parentId
            if (parent != null) {
                val cached = childrenCache[parent]
                if (cached != null) {
                    childrenCache[parent] = cached.filterNot { child -> child.id == id }
                }
            }
        } else {
            val entry = childrenCache.entries.firstOrNull { e -> e.value.any { child -> child.id == id } } ?: return
            val found = entry.value.first { child -> child.id == id }
            movedNode = found.copy(absolutePath = newAbsolutePath)
            childrenCache[entry.key] = entry.value.filterNot { child -> child.id == id }
        }

        val insertedVn = if (newParentId == null) addRoot(movedNode) else insertChild(newParentId, movedNode)
        if (insertedVn != null && expanded[id] == true) {
            childrenCache[id]?.let { insertChildrenRecursive(insertedVn, it) }
        }
    }

    private fun insertChildrenRecursive(vn: VisibleFileNode, children: List<FileNode>) {
        insertChildren(vn, children)
        children.forEach { child ->
            if (expanded[child.id] == true) {
                childrenCache[child.id]?.let {
                    insertChildrenRecursive(VisibleFileNode(child, vn.depth + 1, vn.node.id), it)
                }
            }
        }
    }

    fun toggle(vn: VisibleFileNode) {
        if (!vn.node.isDirectory) return
        if (expanded[vn.node.id] == true) collapse(vn) else expand(vn)
    }

    fun refresh(node: FileNode) {
        childrenCache.remove(node.id)
        val vn = visibleNodes.firstOrNull { it.node.id == node.id } ?: return
        if (expanded[node.id] == true) {
            collapse(vn)
            expand(vn)
        }
    }

    private fun expand(vn: VisibleFileNode) {
        val id = vn.node.id
        expanded[id] = true

        val cached = childrenCache[id]
        if (cached != null) {
            insertChildren(vn, cached)
            return
        }
        if (loadState[id] == LoadState.LOADING) return

        val placeholderCount = (vn.node.childCount ?: 0).coerceAtLeast(0)
        if (placeholderCount > 0) insertPlaceholders(vn, placeholderCount)

        loadState[id] = LoadState.LOADING
        pendingJobs[id] = scope.launch {
            val result = try {
                childrenLoader(vn.node)
            } catch (t: Throwable) {
                if (t is CancellationException) ensureActive()
                loadState[id] = LoadState.ERROR
                if (placeholderCount > 0) removePlaceholders(id)
                return@launch
            }
            childrenCache[id] = result
            loadState[id] = LoadState.LOADED
            if (expanded[id] == true) {
                if (placeholderCount > 0) replacePlaceholders(id, result) else insertChildren(vn, result)
            }
        }
    }

    private fun insertPlaceholders(vn: VisibleFileNode, count: Int) {
        val index = visibleNodes.indexOfFirst { it.node.id == vn.node.id }
        if (index < 0) return
        val depth = vn.depth + 1
        val placeholders = List(count) { i ->
            VisibleFileNode(
                node = FileNode(
                    id = "${vn.node.id}/__placeholder__$i",
                    name = "",
                    absolutePath = "",
                    isDirectory = false,
                    isPlaceholder = true,
                ),
                depth = depth,
                parentId = vn.node.id,
            )
        }
        visibleNodes.addAll(index + 1, placeholders)
    }

    private fun placeholderRange(parentId: String): IntRange? {
        val index = visibleNodes.indexOfFirst { it.node.id == parentId }
        if (index < 0) return null
        val depth = visibleNodes[index].depth
        var end = index + 1
        while (end < visibleNodes.size && visibleNodes[end].depth > depth && visibleNodes[end].node.isPlaceholder) end++
        return if (end > index + 1) (index + 1) until end else null
    }

    private fun removePlaceholders(parentId: String) {
        placeholderRange(parentId)?.let { visibleNodes.subList(it.first, it.last + 1).clear() }
    }

    private fun replacePlaceholders(parentId: String, result: List<FileNode>) {
        val index = visibleNodes.indexOfFirst { it.node.id == parentId }
        if (index < 0) return
        val depth = visibleNodes[index].depth
        val range = placeholderRange(parentId)
        val newItems = result.map { VisibleFileNode(it, depth + 1, parentId) }
        if (range != null) visibleNodes.subList(range.first, range.last + 1).clear()
        visibleNodes.addAll(index + 1, newItems)
    }

    private fun collapse(vn: VisibleFileNode) {
        val id = vn.node.id
        if (expanded[id] != true) return
        expanded[id] = false
        pendingJobs.remove(id)?.cancel()

        val startIndex = visibleNodes.indexOfFirst { it.node.id == id }
        if (startIndex < 0) return

        val parentDepth = vn.depth
        var endIndex = startIndex + 1
        while (endIndex < visibleNodes.size && visibleNodes[endIndex].depth > parentDepth) endIndex++
        if (endIndex > startIndex + 1) {
            visibleNodes.subList(startIndex + 1, endIndex).clear()
        }
    }

    private fun insertChildren(vn: VisibleFileNode, children: List<FileNode>) {
        val index = visibleNodes.indexOfFirst { it.node.id == vn.node.id }
        if (index < 0) return
        val childDepth = vn.depth + 1
        val newItems = children.map { VisibleFileNode(it, childDepth, parentId = vn.node.id) }
        visibleNodes.addAll(index + 1, newItems)
    }
}

@Composable
fun rememberFileTreeState(childrenLoader: ChildrenLoader): FileTreeState {
    val scope = rememberCoroutineScope()
    return remember(childrenLoader) { FileTreeState(scope, childrenLoader) }
}
