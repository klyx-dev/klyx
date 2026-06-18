package com.klyx.presentation.viewmodel

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.data.file.FileRepository
import com.klyx.data.file.KxFile
import com.klyx.data.file.wrap
import com.klyx.data.project.ProjectRepository
import com.klyx.presentation.components.filetree.FileNode
import com.klyx.presentation.components.filetree.FlatNode
import com.klyx.util.tryOrNull
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@Immutable
data class FileTreeUiState(
    val rootNodes: ImmutableList<FileNode> = persistentListOf(),
    val isRefreshing: Boolean = false,
    val selectedNode: FileNode? = null,
    val expandedNodes: ImmutableSet<FileNode> = persistentSetOf(),
    val loadingNodes: ImmutableSet<FileNode> = persistentSetOf(),
    val childrenNodeCache: ImmutableMap<FileNode, ImmutableList<FileNode>> = persistentMapOf()
) {
    fun isNodeExpanded(node: FileNode) = expandedNodes.contains(node)
    fun isNodeSelected(node: FileNode) = selectedNode == node
    fun isLoading(node: FileNode) = loadingNodes.contains(node)
}

sealed interface ClipboardState {
    val node: FileNode

    data class Copy(override val node: FileNode) : ClipboardState
    data class Cut(override val node: FileNode) : ClipboardState
}

@KoinViewModel
class FileTreeViewModel(
    private val fileRepository: FileRepository,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "FileTreeViewModel"
    }

    private val _uiState = MutableStateFlow(FileTreeUiState())
    val uiState: StateFlow<FileTreeUiState> = _uiState.asStateFlow()

    private val _clipboardState = MutableStateFlow<ClipboardState?>(null)
    val clipboardState: StateFlow<ClipboardState?> = _clipboardState.asStateFlow()

    val visibleNodes = uiState.map { state ->
        buildVisibleNodes(state)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        restoreSession()
    }

    @SuppressLint("UseKtx")
    private fun restoreSession() {
        viewModelScope.launch {
            projectRepository
                .recentProjects()
                .forEach {
                    val uri = Uri.parse(it.uri)
                    val file = withContext(Dispatchers.IO) { fileRepository.wrapUri(uri) }
                    val node = tryOrNull { FileNode(file) }
                    if (node != null) {
                        addRootNode(node)
                        if (it.isExpanded) expandNode(node) else collapseNode(node)
                    } else {
                        projectRepository.removeRecent(uri)
                    }
                }
        }
    }

    private fun buildVisibleNodes(
        state: FileTreeUiState
    ): List<FlatNode> {
        val start = System.nanoTime()
        val result = mutableListOf<FlatNode>()

        fun dfs(node: FileNode, depth: Int, currentRoot: FileNode) {
            result += FlatNode(node, depth, currentRoot)

            if (state.expandedNodes.contains(node)) {
                state.childrenNodeCache[node]?.forEach { child ->
                    dfs(child, depth + 1, currentRoot)
                }
            }
        }

        state.rootNodes.forEach { rootNode ->
            dfs(rootNode, 0, rootNode)
        }

        Log.d(
            "FileTree",
            "visibleNodes=${result.size} took ${(System.nanoTime() - start) / 1_000_000} ms"
        )
        return result
    }

    fun copyNode(node: FileNode) {
        _clipboardState.value = ClipboardState.Copy(node)
    }

    fun cutNode(node: FileNode) {
        _clipboardState.value = ClipboardState.Cut(node)
    }

    fun clearClipboard() {
        _clipboardState.value = null
    }

    fun pasteNode(
        targetParent: FileNode,
        clipboardUri: Uri? = null,
        onMoveCompleted: (oldUri: Uri, newUri: Uri) -> Unit = { _, _ -> }
    ) {
        val clipboard = _clipboardState.value
        val sourceNode = clipboard?.node ?: clipboardUri?.let(::FileNode) ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val success = when (clipboard) {
                is ClipboardState.Copy, null -> {
                    val result = fileRepository.copy(sourceNode.uri, targetParent.uri) != null
                    if (result) {
                        invalidateParentCache(targetParent)
                    }
                    result
                }

                is ClipboardState.Cut -> {
                    val result = sourceNode.parent?.uri?.let {
                        fileRepository.move(sourceNode.uri, it, targetParent.uri) != null
                    } ?: (fileRepository.move(
                        sourceNode.uri,
                        sourceNode.uri,
                        targetParent.uri
                    ) != null)

                    if (result) {
                        sourceNode.parent?.let { invalidateParentCache(it) }
                        if (isRootNode(sourceNode)) {
                            val newUri = targetParent.uri.buildUpon()
                                .appendPath(sourceNode.uri.lastPathSegment).build()
                            onMoveCompleted(sourceNode.uri, newUri)
                            projectRepository.removeRecent(sourceNode.uri)
                            projectRepository.addRecent(newUri, isNodeExpanded(sourceNode))
                        }
                        _uiState.update {
                            it.copy(
                                childrenNodeCache = it.childrenNodeCache.let { cache ->
                                    val updated = cache.mapValues { (_, children) ->
                                        children.filter { child -> child != sourceNode }
                                            .toImmutableList()
                                    }.toMutableMap()
                                    updated.remove(sourceNode)
                                    updated.toImmutableMap()
                                },
                                rootNodes = (it.rootNodes - sourceNode).toImmutableList(),
                                expandedNodes = (it.expandedNodes - sourceNode).toImmutableSet(),
                                loadingNodes = (it.loadingNodes - sourceNode).toImmutableSet(),
                                selectedNode = if (it.selectedNode == sourceNode) null else it.selectedNode
                            )
                        }
                    }
                    result
                }
            }

            if (success) {
                if (clipboard is ClipboardState.Cut) clearClipboard()
                invalidateParentCache(targetParent)
                if (!isNodeExpanded(targetParent)) expandNode(targetParent)
            }
        }
    }

    fun updateRootNodes(nodes: List<FileNode>) {
        _uiState.update { it.copy(rootNodes = nodes.toImmutableList()) }
        nodes.forEach(::loadChildren)
    }

    fun addRootNode(uri: Uri) {
        if (_uiState.value.rootNodes.any { it.uri == uri }) return

        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { fileRepository.wrapUri(uri) }
            addRootNode(FileNode(file))
        }
    }

    fun addRootNode(node: FileNode) {
        if (node in _uiState.value.rootNodes) return

        viewModelScope.launch {
            loadChildren(node)

            if (_uiState.value.rootNodes.isEmpty()) {
                expandNode(node)
            }
            launch { projectRepository.addRecent(node.uri, isNodeExpanded(node)) }

            _uiState.update {
                it.copy(rootNodes = (it.rootNodes + node).toImmutableList())
            }
        }
    }

    fun removeRootNode(file: KxFile) {
        _uiState.value.rootNodes.find { it.file == file }?.let {
            removeRootNode(it)
        }
    }

    fun removeRootNode(node: FileNode) {
        if (_uiState.value.rootNodes.contains(node)) {
            _uiState.update {
                it.copy(rootNodes = (it.rootNodes - node).toImmutableList())
            }
            viewModelScope.launch { projectRepository.removeRecent(node.uri) }
        }
    }

    fun selectNode(node: FileNode) {
        _uiState.update { it.copy(selectedNode = node) }
    }

    fun expandNode(node: FileNode) {
        _uiState.update { it.copy(expandedNodes = (it.expandedNodes + node).toImmutableSet()) }
        loadChildren(node)
    }

    fun collapseNode(node: FileNode) {
        _uiState.update {
            it.copy(
                expandedNodes = (it.expandedNodes - node).toImmutableSet()
            )
        }
    }

    fun toggleNode(node: FileNode) {
        if (isNodeExpanded(node)) {
            collapseNode(node)
        } else {
            expandNode(node)
        }
    }

    fun isNodeExpanded(node: FileNode) = _uiState.value.expandedNodes.contains(node)
    fun isNodeSelected(node: FileNode) = _uiState.value.selectedNode == node

    fun isRootNode(node: FileNode) = _uiState.value.rootNodes.contains(node)
    fun isLoading(node: FileNode) = _uiState.value.loadingNodes.contains(node)

    fun loadChildren(
        parent: FileNode,
        onFailure: (Throwable) -> Unit = {},
        force: Boolean = false
    ) {
        if (isLoading(parent)) return
        val currentState = _uiState.value

        if (!force && currentState.childrenNodeCache.contains(parent)) {
            return
        }

        _uiState.update {
            it.copy(loadingNodes = (it.loadingNodes + parent).toImmutableSet())
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val childNodes = projectRepository
                    .listFiles(parent.uri)
                    .map(::FileNode)
                    .toImmutableList()

                _uiState.update {
                    it.copy(
                        childrenNodeCache = (it.childrenNodeCache + (parent to childNodes)).toImmutableMap()
                    )
                }
            } catch (t: Throwable) {
                onFailure(t)
            } finally {
                _uiState.update {
                    it.copy(loadingNodes = (it.loadingNodes - parent).toImmutableSet())
                }
            }
        }
    }

    fun childrenOf(parent: FileNode) =
        _uiState.value.childrenNodeCache[parent] ?: persistentListOf()

    fun renameNode(
        node: FileNode,
        newName: String,
        onSuccess: (newUri: Uri) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val newUri = fileRepository.rename(node.uri, newName)

                if (newUri != null) {
                    val newNode = FileNode(newUri)

                    val selectedNode = if (_uiState.value.selectedNode == node) {
                        newNode
                    } else {
                        _uiState.value.selectedNode
                    }

                    if (isRootNode(node)) {
                        projectRepository.removeRecent(node.uri)
                        projectRepository.addRecent(newUri, isNodeExpanded(node))
                    }

                    _uiState.update { it ->
                        it.copy(
                            rootNodes = it.rootNodes.map { root -> if (root == node) newNode else root }
                                .toImmutableList(),
                            selectedNode = selectedNode,

                            expandedNodes = if (it.expandedNodes.contains(node)) {
                                ((it.expandedNodes - node) + newNode).toImmutableSet()
                            } else {
                                it.expandedNodes
                            },

                            loadingNodes = if (it.loadingNodes.contains(node)) {
                                ((it.loadingNodes - node) + newNode).toImmutableSet()
                            } else {
                                it.loadingNodes
                            },

                            childrenNodeCache = it.childrenNodeCache.let { cache ->
                                val updatedCache = cache.mapValues { (_, children) ->
                                    children.map { child -> if (child == node) newNode else child }
                                        .toImmutableList()
                                }.toMutableMap()
                                cache[node]?.let { children ->
                                    updatedCache[newNode] = children
                                    updatedCache.remove(node)
                                }
                                updatedCache.toImmutableMap()
                            }
                        )
                    }

                    onSuccess(newUri)
                    node.parent?.let { invalidateParentCache(it) }
                } else {
                    onError("Failed to rename. Name might already exist or file is locked.")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "An unexpected error occurred while renaming.")
            }
        }
    }

    fun deleteNode(
        node: FileNode,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val success = fileRepository.delete(node.uri)

                if (success) {
                    if (isRootNode(node)) {
                        projectRepository.removeRecent(node.uri)
                    }

                    _uiState.update {
                        it.copy(
                            rootNodes = (it.rootNodes - node).toImmutableList(),
                            expandedNodes = (it.expandedNodes - node).toImmutableSet(),
                            loadingNodes = (it.loadingNodes - node).toImmutableSet(),
                            selectedNode = if (it.selectedNode == node) null else it.selectedNode
                        )
                    }

                    node.parent?.let { invalidateParentCache(it) }
                    onSuccess()
                } else {
                    onError("Failed to delete. The file might be in use or permissions are denied.")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "An unexpected error occurred while deleting.")
            }
        }
    }

    fun createFile(
        parent: FileNode,
        fileName: String,
        onSuccess: (KxFile) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val newUri = fileRepository.createFile(parent.uri, fileName)

                if (newUri != null) {
                    onSuccess(newUri.wrap())
                    invalidateParentCache(parent)

                    if (!isNodeExpanded(parent)) {
                        expandNode(parent)
                    }
                } else {
                    onError("Failed to create file. Check your permissions.")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to create file.")
            }
        }
    }

    fun createFolder(
        parent: FileNode,
        folderName: String,
        onSuccess: (KxFile) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val newUri = fileRepository.createDirectory(parent.uri, folderName)

                if (newUri != null) {
                    onSuccess(newUri.wrap())
                    invalidateParentCache(parent)

                    if (!isNodeExpanded(parent)) {
                        expandNode(parent)
                    }
                } else {
                    onError("Failed to create directory.")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to create directory.")
            }
        }
    }

    private fun invalidateParentCache(parent: FileNode) {
        _uiState.update {
            it.copy(childrenNodeCache = (it.childrenNodeCache - parent).toImmutableMap())
        }

        if (isNodeExpanded(parent)) {
            loadChildren(
                parent = parent,
                force = true,
                onFailure = { t -> Log.e(TAG, "Failed to reload children for ${parent.uri}", t) }
            )
        }
    }

    fun refreshTree(
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                _uiState.update {
                    it.copy(childrenNodeCache = persistentMapOf())
                }

                _uiState.value.rootNodes.forEach { node ->
                    loadChildren(node, force = true)
                }

                _uiState.value.expandedNodes.forEach { node ->
                    loadChildren(node, force = true)
                }

                onSuccess()
            } catch (t: Throwable) {
                onFailure(t)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshNode(
        node: FileNode,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                invalidateParentCache(node)

                if (isNodeExpanded(node)) {
                    loadChildren(node, force = true)
                }
                onSuccess()
            } catch (t: Throwable) {
                onFailure(t)
            }
        }
    }
}
