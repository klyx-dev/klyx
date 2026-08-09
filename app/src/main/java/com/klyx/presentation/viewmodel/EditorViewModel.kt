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
import com.klyx.data.preferences.SettingsRepository
import com.klyx.data.repository.PluginTabRepository
import com.klyx.data.repository.RecentFileRepository
import com.klyx.event.eventBus
import com.klyx.lsp.LspManager
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.compose.writeTextTo
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val unsupportedFileAlert: UnsupportedFileAlert? = null,
    val pendingCloseTabId: String? = null
)

sealed interface EditorEvent {
    data class ShowError(val error: String) : EditorEvent
    data class ShowMessage(val message: String) : EditorEvent
}

@OptIn(UnsafeGlobalAccess::class)
@KoinViewModel
class EditorViewModel(
    private val fileSystem: FileSystem,
    private val settingsRepository: SettingsRepository,
    private val recentFileRepository: RecentFileRepository,
    private val editorStateRegistry: EditorStateRegistry,
    private val lspManager: LspManager,
    private val pluginTabRepository: PluginTabRepository,
) : ViewModel() {

    companion object {
        var tabIdToSkipOnRestore: String? = null
    }

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
        val skipId = tabIdToSkipOnRestore
        tabIdToSkipOnRestore = null
        viewModelScope.launch {
            recentFileRepository
                .getRecentFiles()
                .forEach { entity ->
                    if (entity.uri == skipId) return@forEach
                    openFile(
                        uri = Uri.parse(entity.uri),
                        projectUri = entity.projectUri?.let { uri -> Uri.parse(uri) }
                    )
                }
            if (skipId != null) {
                recentFileRepository.removeByUri(Uri.parse(skipId))
                pluginTabRepository.removeTab(skipId)
            }
        }
    }

    private fun sendEvent(event: EditorEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private var fontSizeJob: Job? = null
    fun updateFontSize(newSize: Float) {
        fontSizeJob?.cancel()
        fontSizeJob = viewModelScope.launch {
            settingsRepository.updateFontSize(newSize)
        }
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
        if (tab is WorkspaceTab.Custom) {
            val pluginId = tab.pluginId
            if (pluginId != null) {
                viewModelScope.launch {
                    pluginTabRepository.saveTab(tab.id, pluginId, tab.title)
                }
            }
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
        val tab = _uiState.value.openTabs.find { it.id == tabId }
        if (tab is WorkspaceTab.TextFile && tab.hasUnsavedChanges) {
            _uiState.update { it.copy(pendingCloseTabId = tabId) }
        } else {
            forceCloseTab(tabId)
        }
    }

    private fun forceCloseTab(tabId: String) {
        editorStateRegistry.unregister(tabId)
        lspManager.onEditorClosed(tabId)
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
                    is WorkspaceTab.Custom -> {
                        closedTab.onClose?.invoke()
                        recentFileRepository.removeByUri(Uri.parse(closedTab.id))
                        pluginTabRepository.removeTab(closedTab.id)
                    }

                    is WorkspaceTab.Welcome -> {}
                }
            }

            state.copy(
                openTabs = newTabs,
                activeTabId = newActiveTab
            )
        }
    }

    fun confirmCloseTab(tabId: String) {
        val tab = _uiState.value.openTabs.find { it.id == tabId } as? WorkspaceTab.TextFile
        if (tab != null) {
            val editorState = editorStateRegistry[tabId]
            if (editorState != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val _ = runCatching {
                        fileSystem.outputStream(tab.file.uri).use { output ->
                            editorState.writeTextTo(output)
                        }
                        val savedText = editorState.text.toString()
                        editorStateRegistry.setBaselineText(tabId, savedText)
                        lspManager.onFileSaved(tabId)
                        _uiState.update { state ->
                            state.copy(
                                openTabs = state.openTabs.mutate { tabs ->
                                    val index = tabs.indexOfFirst { it.id == tabId }
                                    if (index != -1) {
                                        tabs[index] = tab.copy(hasUnsavedChanges = false)
                                    }
                                }
                            )
                        }
                    }
                    forceCloseTab(tabId)
                }
            } else {
                forceCloseTab(tabId)
            }
        }
        _uiState.update { it.copy(pendingCloseTabId = null) }
    }

    fun discardCloseTab(tabId: String) {
        forceCloseTab(tabId)
        _uiState.update { it.copy(pendingCloseTabId = null) }
    }

    fun dismissCloseTab() {
        _uiState.update { it.copy(pendingCloseTabId = null) }
    }

    fun closeOtherTabs(currentTabId: String) {
        val state = _uiState.value
        val closedTabs = state.openTabs.filter { it.id != currentTabId }

        closedTabs.forEach { tab ->
            editorStateRegistry.unregister(tab.id)
            lspManager.onEditorClosed(tab.id)
        }

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
                    is WorkspaceTab.Custom -> {
                        tab.onClose?.invoke()
                        recentFileRepository.removeByUri(Uri.parse(tab.id))
                        pluginTabRepository.removeTab(tab.id)
                    }

                    is WorkspaceTab.Welcome -> {}
                }
            }
        }
    }

    fun closeAllTabs() {
        _uiState.value.openTabs.forEach { tab ->
            editorStateRegistry.unregister(tab.id)
            lspManager.onEditorClosed(tab.id)
        }
        _uiState.update {
            it.copy(
                openTabs = persistentListOf(),
                activeTabId = null
            )
        }

        viewModelScope.launch {
            recentFileRepository.clearAll()
            pluginTabRepository.removeAll()
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
                val tab = createTabForCategory(file = file, uri = uri, category = category, projectUri = projectUri)

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

    /** Builds the [WorkspaceTab] for [uri] based on its [FileCategory], or null to open nothing. */
    private suspend fun createTabForCategory(
        file: KxFile,
        uri: Uri,
        category: FileCategory,
        projectUri: Uri?,
    ): WorkspaceTab? = when (category) {
        FileCategory.TEXT -> {
            val txt = withContext(Dispatchers.IO) {
                fileSystem.inputStream(uri).bufferedReader().use { it.readText() }
            }
            val newTab = WorkspaceTab.TextFile(
                file = file,
                projectUri = projectUri
            )
            editorStateRegistry.setBaselineText(newTab.id, txt)
            newTab
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
                            val updated = renameTabUri(
                                tab = mutableTabs[i],
                                oldUri = oldUri,
                                newUri = newUri,
                                newFile = newFile
                            ) ?: continue

                            if (state.activeTabId == mutableTabs[i].id) {
                                newActiveTabId = updated.id
                            }
                            mutableTabs[i] = updated
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

    /** Returns [tab] with its [oldUri] replaced by [newUri], or null if [tab] isn't affected. */
    private fun renameTabUri(
        tab: WorkspaceTab,
        oldUri: Uri,
        newUri: Uri,
        newFile: KxFile,
    ): WorkspaceTab? = when (tab) {
        is WorkspaceTab.TextFile -> {
            if (tab.file.uri != oldUri) return null
            val updatedTab = tab.copy(
                file = newFile,
                title = newFile.name,
                projectUri = tab.projectUri,
                id = newFile.uri.toString()
            )

            editorStateRegistry.getBaselineText(tab.id)?.let { baseline ->
                editorStateRegistry.setBaselineText(updatedTab.id, baseline)
            }
            updatedTab
        }

        is WorkspaceTab.ImageFile -> {
            if (tab.uri != oldUri) return null
            tab.copy(
                uri = newUri,
                title = newFile.name,
                id = newUri.toString(),
                projectUri = tab.projectUri
            )
        }

        is WorkspaceTab.Welcome -> null
        is WorkspaceTab.Custom -> null
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
            val index = state.openTabs.indexOfFirst { it.id == tabId }
            if (index == -1) return@update state

            val tab = state.openTabs[index] as? WorkspaceTab.TextFile ?: return@update state
            if (tab.hasUnsavedChanges == modified) return@update state

            state.copy(
                openTabs = state.openTabs.mutate { tabs ->
                    tabs[index] = tab.copy(hasUnsavedChanges = modified)
                }
            )
        }
    }

    private fun saveFile(action: Save) {
        viewModelScope.launch {
            if (saveFileSuspending(action.file)) {
                sendEvent(EditorEvent.ShowMessage("Saved ${action.file.name}"))
            }
        }
    }

    /**
     * Saves the active editor tab to [file] and suspends until the write completes.
     *
     * @return `true` if the file was saved successfully, `false` otherwise.
     */
    suspend fun saveFileSuspending(file: KxFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val activeTabId = _uiState.value.activeTabId ?: return@withContext false
            val editorState = activeEditorState ?: return@withContext false

            fileSystem.outputStream(file.uri).use { output ->
                editorState.writeTextTo(output)
            }
            val savedText = editorState.text.toString()

            editorStateRegistry.setBaselineText(activeTabId, savedText)
            lspManager.onFileSaved(activeTabId)

            _uiState.update { state ->
                state.copy(
                    openTabs = state.openTabs.mutate { tabs ->
                        val index = tabs.indexOfFirst { it.id == activeTabId }
                        if (index != -1) {
                            val tab = tabs[index] as? WorkspaceTab.TextFile ?: return@mutate
                            tabs[index] = tab.copy(hasUnsavedChanges = false)
                        }
                    }
                )
            }
            true
        } catch (e: Exception) {
            sendEvent(EditorEvent.ShowError("Failed to save: ${e.localizedMessage}"))
            false
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
                fileSystem.outputStream(action.newFile.uri).use { output ->
                    editorState.writeTextTo(output)
                }

                val newTabId = action.newFile.uri.toString()
                val savedText = editorState.text.toString()

                editorStateRegistry[newTabId] = editorState
                editorStateRegistry.unregister(action.oldTabId)
                lspManager.onEditorClosed(action.oldTabId)

                editorStateRegistry.setBaselineText(newTabId, savedText)

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
