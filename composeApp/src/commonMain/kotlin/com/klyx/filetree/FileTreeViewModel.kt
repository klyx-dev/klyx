package com.klyx.filetree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.extension.api.Worktree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FileTreeViewModel : ViewModel() {
    private val _rootNodes = MutableStateFlow(emptyMap<Worktree, FileTreeNode>())
    val rootNodes = _rootNodes.asStateFlow()

    var selectedNode by mutableStateOf<FileTreeNode?>(null)

    private val expandedNodes = mutableStateListOf<FileTreeNode>()
    private val loadingNodes = mutableStateListOf<FileTreeNode>()

    private val childrenNodeCache = mutableStateMapOf<FileTreeNode, List<FileTreeNode>>()

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
        expandedNodes += node
    }

    fun collapseNode(node: FileTreeNode) {
        expandedNodes -= node
    }

    fun toggleExpandedState(node: FileTreeNode) {
        if (isNodeExpanded(node)) {
            collapseNode(node)
        } else {
            expandNode(node)
        }
    }

    fun isNodeExpanded(node: FileTreeNode): Boolean {
        return expandedNodes.contains(node)
    }

    fun isNodeSelected(node: FileTreeNode): Boolean {
        return selectedNode == node
    }

    fun isRootNode(node: FileTreeNode): Boolean {
        return _rootNodes.value.containsValue(node)
    }

    fun isNodeLoading(node: FileTreeNode): Boolean {
        return loadingNodes.contains(node)
    }

    private fun addLoadingNode(node: FileTreeNode) {
        loadingNodes += node
    }

    private fun removeLoadingNode(node: FileTreeNode) {
        loadingNodes -= node
    }

    fun loadChildren(parent: FileTreeNode) {
        addLoadingNode(parent)
        if (childrenNodeCache.containsKey(parent)) {
            removeLoadingNode(parent)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val childNodes = parent.file
                    .listFiles()
                    .orEmpty()
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    .toFileTreeNodes()

                childrenNodeCache += (parent to childNodes)
            } finally {
                removeLoadingNode(parent)
            }
        }
    }

    fun childrenOf(parent: FileTreeNode) = childrenNodeCache[parent] ?: emptyList()
}
