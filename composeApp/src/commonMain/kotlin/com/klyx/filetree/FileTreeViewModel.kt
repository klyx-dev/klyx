package com.klyx.filetree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.Notifier
import com.klyx.core.file.KxFile
import com.klyx.core.file.resolve
import com.klyx.core.file.toOkioPath
import com.klyx.extension.api.Worktree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.SYSTEM

data class ClipboardState(
    val node: FileTreeNode,
    val isCut: Boolean // true = cut, false = copy
)

class FileTreeViewModel(
    private val notifier: Notifier
) : ViewModel() {
    private val _rootNodes = MutableStateFlow(emptyMap<Worktree, FileTreeNode>())
    val rootNodes = _rootNodes.asStateFlow()

    private val _clipboard = MutableStateFlow<ClipboardState?>(null)
    val clipboard = _clipboard.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var selectedNode by mutableStateOf<FileTreeNode?>(null)

    private val expandedNodes = mutableStateListOf<String>()
    private val loadingNodes = mutableStateListOf<String>()

    private val childrenNodeCache = mutableStateMapOf<String, List<FileTreeNode>>()

    fun copyNode(node: FileTreeNode) {
        _clipboard.update { ClipboardState(node, false) }
    }

    fun cutNode(node: FileTreeNode) {
        _clipboard.update { ClipboardState(node, true) }
    }

    fun clearClipboard() {
        _clipboard.update { null }
    }

    fun hasClip() = _clipboard.value != null

    fun pasteNode(targetNode: FileTreeNode): Boolean {
        val clip = _clipboard.value ?: return false
        if (targetNode.isDirectory.not()) return false

        val node = clip.node
        val newFile = targetNode.file.resolve(node.name)

        val fs = FileSystem.SYSTEM

        return if (clip.isCut) {
            try {
                try {
                    fs.atomicMove(node.file.toOkioPath(), newFile.toOkioPath())
                } catch (_: Exception) {
                    fs.copy(node.file.toOkioPath(), newFile.toOkioPath())
                    fs.delete(node.file.toOkioPath())
                }

                invalidateParentCache(targetNode.file.absolutePath)
                node.file.parentFile?.let { invalidateParentCache(it.absolutePath) }

                if (!isNodeExpanded(targetNode)) {
                    expandNode(targetNode)
                }
                true
            } catch (e: Exception) {
                notifier.toast(e.message ?: "Failed to paste")
                false
            }
        } else {
            try {
                val success = newFile.createNewFile()

                if (success) {
                    fs.copy(node.file.toOkioPath(), newFile.toOkioPath())

                    invalidateParentCache(targetNode.file.absolutePath)

                    if (!isNodeExpanded(targetNode)) {
                        expandNode(targetNode)
                    }
                }

                success
            } catch (_: Exception) {
                false
            }
        }
    }

    fun addRootNode(worktree: Worktree) {
        _rootNodes.update { it + (worktree to worktree.asFileTreeNode()) }
    }

    fun removeRootNode(worktree: Worktree) {
        if (_rootNodes.value.containsKey(worktree)) {
            _rootNodes.update { it - worktree }
        }
    }

    fun updateRootNodes(nodes: Map<Worktree, FileTreeNode>) {
        _rootNodes.update { nodes }
        nodes.forEach { (_, node) -> loadChildren(node) }
    }

    fun selectNode(node: FileTreeNode) {
        selectedNode = node
    }

    fun expandNode(node: FileTreeNode) {
        val path = node.file.absolutePath
        if (!expandedNodes.contains(path)) {
            expandedNodes.add(path)
        }
    }

    fun collapseNode(node: FileTreeNode) {
        expandedNodes.remove(node.file.absolutePath)
    }

    fun toggleExpandedState(node: FileTreeNode) {
        if (isNodeExpanded(node)) {
            collapseNode(node)
        } else {
            expandNode(node)
        }
    }

    fun isNodeExpanded(node: FileTreeNode): Boolean {
        return expandedNodes.contains(node.file.absolutePath)
    }

    fun isNodeSelected(node: FileTreeNode): Boolean {
        return selectedNode?.file?.absolutePath == node.file.absolutePath
    }

    fun isRootNode(node: FileTreeNode): Boolean {
        return _rootNodes.value.values.any { it.file.absolutePath == node.file.absolutePath }
    }

    fun isNodeLoading(node: FileTreeNode): Boolean {
        return loadingNodes.contains(node.file.absolutePath)
    }

    private fun addLoadingNode(node: FileTreeNode) {
        val path = node.file.absolutePath
        if (!loadingNodes.contains(path)) {
            loadingNodes.add(path)
        }
    }

    private fun removeLoadingNode(node: FileTreeNode) {
        loadingNodes.remove(node.file.absolutePath)
    }

    fun loadChildren(parent: FileTreeNode, force: Boolean = false) {
        val parentPath = parent.file.absolutePath

        if (!force && childrenNodeCache.containsKey(parentPath)) {
            return
        }

        addLoadingNode(parent)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val childNodes = parent.file
                    .listFiles()
                    .orEmpty()
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    .toFileTreeNodes()

                childrenNodeCache[parentPath] = childNodes
            } catch (e: Exception) {
                notifier.toast("Failed to load directory: ${e.message}")
            } finally {
                removeLoadingNode(parent)
            }
        }
    }

    fun childrenOf(parent: FileTreeNode) = childrenNodeCache[parent.file.absolutePath] ?: emptyList()

    fun renameNode(node: FileTreeNode, newFile: KxFile): Boolean {
        return try {
            val parent = node.file.parentFile ?: return false
            val success = node.file.renameTo(newFile)

            if (success) {
                val oldPath = node.file.absolutePath
                val newPath = newFile.absolutePath

                if (expandedNodes.contains(oldPath)) {
                    expandedNodes.remove(oldPath)
                    expandedNodes.add(newPath)
                }

                if (loadingNodes.contains(oldPath)) {
                    loadingNodes.remove(oldPath)
                    loadingNodes.add(newPath)
                }

                childrenNodeCache[oldPath]?.let { children ->
                    childrenNodeCache.remove(oldPath)
                    childrenNodeCache[newPath] = children
                }

                if (selectedNode?.file?.absolutePath == oldPath) {
                    selectedNode = FileTreeNode(newFile, newFile.name)
                }

                invalidateParentCache(parent.absolutePath)
            }

            success
        } catch (_: Exception) {
            false
        }
    }

    fun deleteNode(node: FileTreeNode): Boolean {
        return try {
            val success = if (node.file.isDirectory) {
                node.file.deleteRecursively()
            } else {
                node.file.delete()
            }

            if (success) {
                val path = node.file.absolutePath

                expandedNodes.remove(path)
                loadingNodes.remove(path)
                childrenNodeCache.remove(path)

                if (selectedNode?.file?.absolutePath == path) {
                    selectedNode = null
                }

                node.file.parentFile?.let { parent ->
                    invalidateParentCache(parent.absolutePath)
                }
            }

            success
        } catch (_: Exception) {
            false
        }
    }

    fun createNewFile(parentNode: FileTreeNode, fileName: String): Boolean {
        return try {
            val newFile = parentNode.file.resolve(fileName)
            val success = newFile.createNewFile()

            if (success) {
                invalidateParentCache(parentNode.file.absolutePath)

                if (!isNodeExpanded(parentNode)) {
                    expandNode(parentNode)
                }
            }

            success
        } catch (_: Exception) {
            false
        }
    }

    fun createNewFolder(parentNode: FileTreeNode, folderName: String): Boolean {
        return try {
            val newFolder = parentNode.file.resolve(folderName)
            val success = newFolder.mkdirs()

            if (success) {
                invalidateParentCache(parentNode.file.absolutePath)

                if (!isNodeExpanded(parentNode)) {
                    expandNode(parentNode)
                }
            }

            success
        } catch (_: Exception) {
            false
        }
    }

    private fun invalidateParentCache(parentPath: String) {
        childrenNodeCache.remove(parentPath)

        if (expandedNodes.contains(parentPath)) {
            findNodeByPath(parentPath)?.let { parentNode ->
                loadChildren(parentNode, force = true)
            }
        }
    }

    private fun findNodeByPath(path: String): FileTreeNode? {
        _rootNodes.value.values.forEach { rootNode ->
            if (rootNode.file.absolutePath == path) {
                return rootNode
            }
        }

        fun searchInChildren(nodes: List<FileTreeNode>): FileTreeNode? {
            nodes.forEach { node ->
                if (node.file.absolutePath == path) {
                    return node
                }
                childrenNodeCache[node.file.absolutePath]?.let { children ->
                    searchInChildren(children)?.let { found -> return found }
                }
            }
            return null
        }

        childrenNodeCache.values.forEach { children ->
            searchInChildren(children)?.let { return it }
        }

        return null
    }

    fun refreshTree() {
        viewModelScope.launch {
            _isRefreshing.value = true

            try {
                delay(300)

                childrenNodeCache.clear()

                _rootNodes.value.values.forEach { rootNode ->
                    loadChildren(rootNode, force = true)
                }

                val expandedPaths = expandedNodes.toList()
                expandedPaths.forEach { path ->
                    findNodeByPath(path)?.let { node ->
                        loadChildren(node, force = true)
                    }
                }

                notifier.toast("File tree refreshed")
            } catch (e: Exception) {
                notifier.toast("Refresh failed: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshNode(node: FileTreeNode) {
        viewModelScope.launch {
            try {
                invalidateParentCache(node.file.absolutePath)

                if (node.isDirectory && isNodeExpanded(node)) {
                    loadChildren(node, force = true)
                }

                node.file.parentFile?.let { parent ->
                    invalidateParentCache(parent.absolutePath)
                }

                notifier.toast("\"${node.name}\" refreshed")
            } catch (e: Exception) {
                notifier.toast("Failed to refresh: ${e.message}")
            }
        }
    }

    fun refreshExpandedNodes() {
        viewModelScope.launch {
            val expandedPaths = expandedNodes.toList()
            expandedPaths.forEach { path ->
                findNodeByPath(path)?.let { node ->
                    loadChildren(node, force = true)
                }
            }
        }
    }
}
