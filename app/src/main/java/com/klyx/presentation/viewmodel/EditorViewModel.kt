package com.klyx.presentation.viewmodel

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.api.data.editor.EditorAction
import com.klyx.api.data.editor.FileOpenRequest
import com.klyx.api.data.editor.FileOpenerRegistry
import com.klyx.api.data.editor.Save
import com.klyx.api.data.editor.SaveAs
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.fs.FileCategory
import com.klyx.api.data.fs.FileSystem
import com.klyx.api.event.editor.FileOpenedEvent
import com.klyx.api.util.stateInWhileSubscribed
import com.klyx.core.event.EventBus
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.data.editor.EditorStateRegistry
import com.klyx.data.repository.RecentFileRepository
import com.klyx.event.eventBus
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.compose.writeTextTo
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

data class UnsupportedFileAlert(
    val file: KxFile,
    val projectUri: Uri?
)

data class EditorUiState(
    val openTabs: PersistentList<WorkspaceTab> = persistentListOf(),
    val activeTabId: String? = null,
    val unsupportedFileAlert: UnsupportedFileAlert? = null
)

sealed interface EditorEvent {
    data class ShowError(val error: String) : EditorEvent
    data class ShowMessage(val message: String) : EditorEvent
}

@OptIn(UnsafeGlobalAccess::class)
@KoinViewModel
class EditorViewModel(
    private val fileSystem: FileSystem,
    private val recentFileRepository: RecentFileRepository,
    private val editorStateRegistry: EditorStateRegistry
) : ViewModel() {

    private val fileOpenerRegistry: FileOpenerRegistry by lazy {
        GlobalApp.global<FileOpenerRegistry>()
    }
    private val eventBus: EventBus by lazy { GlobalApp.eventBus() }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState = _uiState.asStateFlow()

    val activeTab = _uiState.map { state ->
        state.openTabs.find { it.id == state.activeTabId }
    }.stateInWhileSubscribed(null)

    val openTabs = _uiState.map { it.openTabs }
        .stateInWhileSubscribed(persistentListOf())

    private val _events = Channel<EditorEvent>()
    val events = _events.receiveAsFlow()

    init {
        restoreSession()
    }

    @SuppressLint("UseKtx")
    private fun restoreSession() {
        viewModelScope.launch {
            recentFileRepository
                .getRecentFiles()
                .forEach {
                    openFile(
                        uri = Uri.parse(it.uri),
                        projectUri = it.projectUri?.let { uri -> Uri.parse(uri) }
                    )
                }
        }
    }

    private fun sendEvent(event: EditorEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    fun openTab(tab: WorkspaceTab) {
        _uiState.update { currentState ->
            currentState.copy(
                openTabs = if (currentState.openTabs.any { it.id == tab.id }) {
                    currentState.openTabs
                } else {
                    currentState.openTabs + tab
                },
                activeTabId = tab.id
            )
        }
    }

    fun selectTab(tabId: String) {
        _uiState.update { currentState ->
            currentState.copy(activeTabId = tabId)
        }
    }

    fun selectTabAtIndex(index: Int) {
        val tab = _uiState.value.openTabs.getOrNull(index) ?: return
        selectTab(tab.id)
    }

    fun closeTab(tabId: String) {
        editorStateRegistry.unregister(tabId)
        _uiState.update { state ->
            val tabIndex = state.openTabs.indexOfFirst { it.id == tabId }
            if (tabIndex == -1) return@update state

            val closedTab = state.openTabs[tabIndex]
            val newTabs = state.openTabs.mutate { it.removeAt(tabIndex) }
            val newActiveTab = if (tabId == state.activeTabId) {
                when {
                    newTabs.isEmpty() -> null
                    tabIndex < newTabs.size -> newTabs[tabIndex].id
                    else -> newTabs.last().id
                }
            } else {
                state.activeTabId
            }

            viewModelScope.launch {
                when (closedTab) {
                    is WorkspaceTab.ImageFile -> recentFileRepository.removeByUri(closedTab.uri)
                    is WorkspaceTab.TextFile -> recentFileRepository.removeFile(closedTab.file)
                    else -> {}
                }
            }

            state.copy(
                openTabs = newTabs,
                activeTabId = newActiveTab
            )
        }
    }

    fun closeOtherTabs(currentTabId: String) {
        val state = _uiState.value
        val closedTabs = state.openTabs.filter { it.id != currentTabId }

        closedTabs.forEach { tab -> editorStateRegistry.unregister(tab.id) }

        _uiState.update {
            it.copy(
                openTabs = it.openTabs
                    .filter { tab -> tab.id == currentTabId }
                    .toPersistentList(),
                activeTabId = currentTabId
            )
        }

        viewModelScope.launch {
            closedTabs.forEach { tab ->
                when (tab) {
                    is WorkspaceTab.ImageFile -> recentFileRepository.removeByUri(tab.uri)
                    is WorkspaceTab.TextFile -> recentFileRepository.removeFile(tab.file)
                    else -> {}
                }
            }
        }
    }

    fun closeAllTabs() {
        _uiState.value.openTabs.forEach { tab -> editorStateRegistry.unregister(tab.id) }
        _uiState.update {
            it.copy(
                openTabs = persistentListOf(),
                activeTabId = null
            )
        }

        viewModelScope.launch {
            recentFileRepository.clearAll()
        }
    }

    fun closeActiveTab() {
        _uiState.value.activeTabId?.let { closeTab(it) }
    }

    fun openFile(uri: Uri, projectUri: Uri? = null) {
        viewModelScope.launch {
            try {
                val file = fileSystem.wrapUri(uri)
                val category = fileSystem.determineFileCategory(uri)

                val tab: WorkspaceTab? = when (category) {
                    FileCategory.TEXT -> {
                        val txt = withContext(Dispatchers.IO) { file.readText() }
                        WorkspaceTab.TextFile(
                            file = file,
                            text = txt,
                            projectUri = projectUri
                        )
                    }

                    FileCategory.IMAGE -> {
                        WorkspaceTab.ImageFile(
                            uri = file.uri,
                            title = file.name,
                            projectUri = projectUri
                        )
                    }

                    FileCategory.BINARY_UNSUPPORTED -> {
                        // Ask registered plugin openers before falling back to the alert.
                        val request = FileOpenRequest(
                            uri = uri,
                            fileName = file.name,
                            extension = file.extension.lowercase(),
                            mimeType = fileSystem.mimeType(uri),
                            projectUri = projectUri
                        )
                        fileOpenerRegistry.open(request) ?: run {
                            _uiState.update {
                                it.copy(
                                    unsupportedFileAlert = UnsupportedFileAlert(
                                        file = file,
                                        projectUri = projectUri
                                    )
                                )
                            }
                            recentFileRepository.removeFile(file)
                            null
                        }
                    }

                    FileCategory.ERROR -> {
                        recentFileRepository.removeFile(file)
                        sendEvent(EditorEvent.ShowError("Failed to read file: ${file.name}"))
                        null
                    }
                }

                if (tab != null) {
                    openTab(tab)
                    recentFileRepository.addRecentFile(file, projectUri)
                    eventBus.publish(
                        FileOpenedEvent(
                            uri = uri,
                            fileName = file.name,
                            tabId = tab.id,
                            projectUri = projectUri
                        )
                    )
                }
            } catch (e: Exception) {
                sendEvent(EditorEvent.ShowError("An unexpected error occurred: ${e.localizedMessage}"))
            }
        }
    }

    fun dismissUnsupportedFileDialog() {
        _uiState.update { it.copy(unsupportedFileAlert = null) }
    }

    fun handleFileRenamed(oldUri: Uri, newUri: Uri) {
        viewModelScope.launch {
            try {
                val newFile = fileSystem.wrapUri(newUri)

                val oldTab = _uiState.value.openTabs.find {
                    when (it) {
                        is WorkspaceTab.TextFile -> it.file.uri == oldUri
                        is WorkspaceTab.ImageFile -> it.uri == oldUri
                        else -> false
                    }
                }

                if (oldTab is WorkspaceTab.TextFile) {
                    recentFileRepository.removeFile(oldTab.file)
                    recentFileRepository.addRecentFile(newFile, oldTab.projectUri)
                }

                if (oldTab is WorkspaceTab.ImageFile) {
                    recentFileRepository.removeByUri(oldTab.uri)
                    recentFileRepository.addRecentFile(newFile, oldTab.projectUri)
                }

                _uiState.update { state ->
                    var newActiveTabId = state.activeTabId

                    val updatedTabs = state.openTabs.mutate { mutableTabs ->
                        for (i in mutableTabs.indices) {
                            when (val tab = mutableTabs[i]) {
                                is WorkspaceTab.TextFile -> {
                                    if (tab.file.uri == oldUri) {
                                        val updatedTab = tab.copy(
                                            file = newFile,
                                            title = newFile.name,
                                            projectUri = tab.projectUri,
                                            id = newFile.uri.toString()
                                        )

                                        if (state.activeTabId == tab.id) {
                                            newActiveTabId = updatedTab.id
                                        }
                                        mutableTabs[i] = updatedTab
                                    }
                                }

                                is WorkspaceTab.ImageFile -> {
                                    if (tab.uri == oldUri) {
                                        val updatedTab = tab.copy(
                                            uri = newUri,
                                            title = newFile.name,
                                            id = newUri.toString(),
                                            projectUri = tab.projectUri
                                        )

                                        if (state.activeTabId == tab.id) {
                                            newActiveTabId = updatedTab.id
                                        }
                                        mutableTabs[i] = updatedTab
                                    }
                                }

                                is WorkspaceTab.Welcome -> {}
                                is WorkspaceTab.Custom -> {}
                            }
                        }
                    }

                    state.copy(
                        openTabs = updatedTabs,
                        activeTabId = newActiveTabId
                    )
                }
            } catch (e: Exception) {
                sendEvent(EditorEvent.ShowError("Failed to sync renamed file in editor: ${e.localizedMessage}"))
            }
        }
    }

    fun handleFileDeleted(deletedUri: Uri) {
        val tabIdToClose = _uiState.value.openTabs.find { tab ->
            when (tab) {
                is WorkspaceTab.TextFile -> tab.file.uri == deletedUri
                is WorkspaceTab.ImageFile -> tab.uri == deletedUri
                else -> false
            }
        }?.id

        tabIdToClose?.let { closeTab(it) }
    }

    fun handleEditorActions(action: EditorAction) {
        when (action) {
            is Save -> saveFile(action)
            is SaveAs -> saveFileAs(action)
        }
    }

    fun editorStateForTab(tabId: String): CodeEditorState? =
        editorStateRegistry[tabId]

    val activeEditorState: CodeEditorState?
        get() = _uiState.value.activeTabId?.let(editorStateRegistry::get)

    fun markTabModified(tabId: String, modified: Boolean) {
        _uiState.update { state ->
            val updatedTabs = state.openTabs.mutate { tabs ->
                val index = tabs.indexOfFirst { it.id == tabId }
                if (index != -1) {
                    val tab = tabs[index] as? WorkspaceTab.TextFile ?: return@mutate
                    tabs[index] = tab.copy(hasUnsavedChanges = modified)
                }
            }
            state.copy(openTabs = updatedTabs)
        }
    }

    private fun saveFile(action: Save) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeTabId = _uiState.value.activeTabId ?: return@launch
            val editorState = activeEditorState ?: run {
                sendEvent(EditorEvent.ShowError("Editor state not available"))
                return@launch
            }

            try {
                editorState.writeTextTo(action.file.outputStream())
                markTabModified(activeTabId, false)
                sendEvent(EditorEvent.ShowMessage("Saved ${action.file.name}"))
            } catch (e: Exception) {
                sendEvent(EditorEvent.ShowError("Failed to save: ${e.localizedMessage}"))
            }
        }
    }

    private fun saveFileAs(action: SaveAs) {
        viewModelScope.launch(Dispatchers.IO) {
            val editorState = editorStateRegistry[action.oldTabId] ?: run {
                sendEvent(EditorEvent.ShowError("Editor state not available"))
                return@launch
            }

            try {
                val oldTab =
                    _uiState.value.openTabs.find { it.id == action.oldTabId } as? WorkspaceTab.TextFile
                editorState.writeTextTo(action.newFile.outputStream())

                val newTabId = action.newFile.uri.toString()

                editorStateRegistry[newTabId] = editorState
                editorStateRegistry.unregister(action.oldTabId)

                recentFileRepository.addRecentFile(action.newFile, oldTab?.projectUri)

                _uiState.update { state ->
                    val updatedTabs = state.openTabs.mutate { tabs ->
                        val index = tabs.indexOfFirst { it.id == action.oldTabId }
                        if (index != -1 && oldTab != null) {
                            tabs[index] = oldTab.copy(
                                id = newTabId,
                                title = action.newFile.name,
                                file = action.newFile,
                                hasUnsavedChanges = false
                            )
                        }
                    }
                    state.copy(
                        openTabs = updatedTabs,
                        activeTabId = newTabId
                    )
                }

                sendEvent(EditorEvent.ShowMessage("Saved as ${action.newFile.name}"))
            } catch (e: Exception) {
                sendEvent(EditorEvent.ShowError("Failed to save as: ${e.localizedMessage}"))
            }
        }
    }
}
