@file:OptIn(ExperimentalCodeEditorApi::class)

package com.klyx.viewmodel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.Notifier
import com.klyx.core.event.EventBus
import com.klyx.core.event.io.FileCloseEvent
import com.klyx.core.event.io.FileOpenEvent
import com.klyx.core.event.io.FileSaveEvent
import com.klyx.core.file.FileId
import com.klyx.core.file.KxFile
import com.klyx.core.file.Worktree
import com.klyx.core.file.id
import com.klyx.core.file.isKlyxTempFile
import com.klyx.core.file.isPermissionRequired
import com.klyx.core.file.isValidUtf8
import com.klyx.core.file.sink
import com.klyx.core.file.toKxFile
import com.klyx.core.io.Paths
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.core.io.globalSettingsFile
import com.klyx.core.io.settingsFile
import com.klyx.core.settings.SettingsManager
import com.klyx.core.util.string
import com.klyx.editor.ComposeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.editor.SoraEditorState
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.ExperimentalComposeCodeEditorApi
import com.klyx.editor.compose.event.TextChangeEvent
import com.klyx.editor.event.ContentChangeEvent
import com.klyx.res.Res.string
import com.klyx.res.tab_title_default_settings
import com.klyx.res.tab_title_extensions
import com.klyx.tab.ComposableTab
import com.klyx.tab.FileTab
import com.klyx.tab.Tab
import com.klyx.tab.TabId
import com.klyx.tab.UnsupportedFileTab
import com.klyx.ui.component.extension.ExtensionScreen
import com.klyx.ui.component.log.LogViewerScreen
import com.klyx.ui.page.createWelcomePage
import com.klyx.viewmodel.util.stateInWhileSubscribed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

data class TabState(
    val openTabs: List<Tab> = emptyList(),
    val activeTabId: TabId? = null,
    val pendingFiles: List<KxFile> = emptyList()
)

@Suppress("MemberVisibilityCanBePrivate", "unused")
class EditorViewModel(
    private val notifier: Notifier
) : ViewModel() {
    private val _state = MutableStateFlow(TabState())
    val state = _state.asStateFlow()

    val activeFile = _state.map { tabState ->
        (tabState.openTabs.find { it is FileTab && it.id == tabState.activeTabId } as? FileTab)?.file
    }.stateInWhileSubscribed(initialValue = null)

    val activeTab = _state.map { tabState ->
        tabState.openTabs.find { it.id == tabState.activeTabId }
    }.stateInWhileSubscribed(null)

    val currentEditorState = _state.map { tabState ->
        (tabState.openTabs.find { it is FileTab && it.id == tabState.activeTabId } as? FileTab)?.editorState
    }.stateInWhileSubscribed(initialValue = null)

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    val isTabOpen = _state.map {
        it.openTabs.isNotEmpty()
    }.stateInWhileSubscribed(initialValue = false)

    init {
        openTab(
            ComposableTab(
                name = "Welcome",
                content = createWelcomePage()
            )
        )

        observeUndoRedoState()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeUndoRedoState() {
        viewModelScope.launch {
            currentEditorState.flatMapLatest { editorState ->
                if (editorState != null) {
                    callbackFlow {
                        when (editorState) {
                            is ComposeEditorState -> {
                                editorState.state.subscribeEvent<TextChangeEvent> {
                                    trySend(Unit)
                                }
                            }

                            is SoraEditorState -> {
                                editorState.state.subscribeEvent<ContentChangeEvent> {
                                    trySend(Unit)
                                }
                            }
                        }

                        trySend(Unit)

                        awaitClose()
                    }.map {
                        val canUndo = when (editorState) {
                            is ComposeEditorState -> editorState.state.canUndo()
                            is SoraEditorState -> editorState.state.canUndo()
                        }
                        val canRedo = when (editorState) {
                            is ComposeEditorState -> editorState.state.canRedo()
                            is SoraEditorState -> editorState.state.canRedo()
                        }

                        canUndo to canRedo
                    }.distinctUntilChanged()
                } else {
                    flowOf(false to false)
                }
            }.collectLatest { (canUndo, canRedo) ->
                _canUndo.update { canUndo }
                _canRedo.update { canRedo }
            }
        }
    }

    private fun updateUndoRedoState() = when (val s = currentEditorState.value) {
        is ComposeEditorState -> {
            val u = _canUndo.updateAndGet { s.state.canUndo() }
            val r = _canRedo.updateAndGet { s.state.canRedo() }
            u to r
        }

        is SoraEditorState -> {
            val u = _canUndo.updateAndGet { s.state.canUndo() }
            val r = _canRedo.updateAndGet { s.state.canRedo() }
            u to r
        }

        null -> false to false
    }

    fun undo() {
        when (val s = currentEditorState.value) {
            is ComposeEditorState -> s.state.undo()
            is SoraEditorState -> s.state.undo()
            null -> {}
        }
        updateUndoRedoState()
    }

    fun redo() {
        when (val s = currentEditorState.value) {
            is ComposeEditorState -> s.state.redo()
            is SoraEditorState -> s.state.redo()
            null -> {}
        }
        updateUndoRedoState()
    }

    fun openTab(tab: Tab) {
        _state.update { current ->
            if (current.openTabs.any { it.id == tab.id }) {
                current.copy(activeTabId = tab.id)
            } else {
                current.copy(
                    openTabs = current.openTabs + tab,
                    activeTabId = tab.id
                )
            }
        }
    }

    fun openTab(
        name: String,
        id: TabId? = null,
        data: Any? = null,
        content: @Composable () -> Unit,
    ) {
        val tab = ComposableTab(
            id = id ?: name,
            name = name,
            data = data,
            content = content
        )

        openTab(tab)
    }

    fun openPendingFiles() {
        _state.value.pendingFiles.forEach(::openFile)
        _state.update { it.copy(pendingFiles = emptyList()) }
    }

    fun isPendingFile(file: KxFile) = file in _state.value.pendingFiles

    @OptIn(ExperimentalComposeCodeEditorApi::class)
    fun openFile(
        file: KxFile,
        worktree: Worktree? = null,
        tabTitle: String = file.name,
        isInternal: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val permissionRequired = file.isPermissionRequired(R_OK or W_OK)
            if (permissionRequired) {
                _state.update { it.copy(pendingFiles = it.pendingFiles + file) }
            }

            val tab = if (!file.isKlyxTempFile() && !permissionRequired && !file.isValidUtf8()) {
                //notifier.error("(${file.name}) stream did not contain valid UTF-8")
                UnsupportedFileTab(
                    id = file.id,
                    name = tabTitle,
                    file = file
                )
            } else {
                FileTab(
                    id = file.id,
                    name = tabTitle,
                    worktree = worktree,
                    isInternal = isInternal,
                    file = file,
                    editorState = if (SettingsManager.settings.value.useComposeEditorInsteadOfSoraEditor) {
                        ComposeEditorState(CodeEditorState(file))
                    } else {
                        SoraEditorState(com.klyx.editor.CodeEditorState(file))
                    }
                )
            }

            openTab(tab)
            EventBus.INSTANCE.post(FileOpenEvent(file, worktree?.rootFile))
        }
    }

    fun closeOthersTab(currentTabId: TabId) {
        _state.update {
            val closingTabs = it.openTabs.filterNot { tab -> tab.id == currentTabId }
            val newTabs = it.openTabs.filter { tab -> tab.id == currentTabId }
            viewModelScope.launch(Dispatchers.Default) {
                closingTabs.forEach { tab ->
                    if (tab is FileTab) {
                        onCloseFileTab(tab.worktree, tab.file)
                        EventBus.INSTANCE.post(FileCloseEvent(tab.file, tab.worktree?.rootFile))
                    }
                }
            }
            it.copy(openTabs = newTabs)
        }
    }

    fun closeLeftTab(currentTabId: TabId) {
        _state.update { state ->
            val tabs = state.openTabs
            val currentIndex = tabs.indexOfFirst { it.id == currentTabId }
            if (currentIndex <= 0) return@update state // nothing to close on the left or tab not found

            val closingTabs = tabs.subList(0, currentIndex)
            val newTabs = tabs.subList(currentIndex, tabs.size)

            viewModelScope.launch(Dispatchers.Default) {
                closingTabs.forEach { tab ->
                    if (tab is FileTab) {
                        onCloseFileTab(tab.worktree, tab.file)
                        EventBus.INSTANCE.post(FileCloseEvent(tab.file, tab.worktree?.rootFile))
                    }
                }
            }

            state.copy(openTabs = newTabs)
        }
    }

    fun closeRightTab(currentTabId: TabId) {
        _state.update { state ->
            val tabs = state.openTabs
            val currentIndex = tabs.indexOfFirst { it.id == currentTabId }
            if (currentIndex == -1 || currentIndex >= tabs.lastIndex) return@update state // nothing to close on right

            val closingTabs = tabs.subList(currentIndex + 1, tabs.size)
            val newTabs = tabs.subList(0, currentIndex + 1)

            viewModelScope.launch(Dispatchers.Default) {
                closingTabs.forEach { tab ->
                    if (tab is FileTab) {
                        onCloseFileTab(tab.worktree, tab.file)
                        EventBus.INSTANCE.post(FileCloseEvent(tab.file, tab.worktree?.rootFile))
                    }
                }
            }

            state.copy(openTabs = newTabs)
        }
    }

    fun canCloseLeftTab(currentTabId: TabId): Boolean {
        val tabs = _state.value.openTabs
        val index = tabs.indexOfFirst { it.id == currentTabId }
        return index > 0 // means there's at least one tab to the left
    }

    fun canCloseRightTab(currentTabId: TabId): Boolean {
        val tabs = _state.value.openTabs
        val index = tabs.indexOfFirst { it.id == currentTabId }
        return index != -1 && index < tabs.lastIndex // means there's at least one tab to the right
    }

    fun getClosableTabSides(currentTabId: TabId): Pair<Boolean, Boolean> {
        val tabs = _state.value.openTabs
        val index = tabs.indexOfFirst { it.id == currentTabId }
        return when {
            index == -1 -> false to false
            else -> (index > 0) to (index < tabs.lastIndex)
        }
    }

    fun closeTab(tabId: TabId) {
        _state.update { current ->
            val updatedTabs = current.openTabs.filterNot { it.id == tabId }
            val newActiveTabId = when {
                tabId == current.activeTabId -> updatedTabs.lastOrNull()?.id
                else -> current.activeTabId
            }

            val tab = current.openTabs.find { it.id == tabId }
            if (tab is FileTab) {
                viewModelScope.launch(Dispatchers.Default) {
                    onCloseFileTab(tab.worktree, tab.file)
                    EventBus.INSTANCE.post(FileCloseEvent(tab.file, tab.worktree?.rootFile))
                }
            }

            current.copy(
                openTabs = updatedTabs,
                activeTabId = newActiveTabId
            )
        }
    }

    fun closeActiveTab() = _state.value.activeTabId?.let { closeTab(it) }

    fun setActiveTab(tabId: TabId) {
        _state.update { current ->
            if (current.openTabs.any { it.id == tabId }) {
                current.copy(activeTabId = tabId)
            } else {
                current
            }
        }
    }

    fun getTab(tabId: TabId): Tab? {
        return _state.value.openTabs.find { it.id == tabId }
    }

    fun replaceTab(tabId: TabId, newTab: Tab) {
        _state.update { current ->
            val updatedTabs = current.openTabs.map { tab ->
                if (tab.id == tabId) newTab else tab
            }

            current.copy(
                openTabs = updatedTabs,
                activeTabId = if (current.activeTabId == tabId) newTab.id else current.activeTabId
            )
        }
    }

    fun closeAllTabs() {
        _state.update { current ->
            current.copy(
                openTabs = emptyList(),
                activeTabId = null
            )
        }
    }

    fun isTabOpen(tabId: TabId): Boolean {
        return _state.value.openTabs.any { it.id == tabId }
    }

    fun isTabActive(tabId: TabId): Boolean {
        return _state.value.activeTabId == tabId
    }

    fun isFileTabOpen(fileId: FileId): Boolean {
        return _state.value.openTabs.any { it is FileTab && it.id == fileId }
    }

    fun isFileTabActive(fileId: FileId): Boolean {
        return _state.value.activeTabId == fileId
    }

    fun isFileTab(tabId: TabId): Boolean {
        return _state.value.openTabs.any { it is FileTab && it.id == tabId }
    }

    fun isFileTabInternal(tabId: TabId): Boolean {
        return _state.value.openTabs.any { it is FileTab && it.isInternal && it.id == tabId }
    }

    fun saveCurrent(): Boolean {
        val current = _state.value
        val tab = current.openTabs.find { it.id == current.activeTabId } ?: return false

        return if (tab is FileTab) {
            val state = tab.editorState
            val file = tab.file

            if (file.path == "untitled") return false

            if (file.canWrite) {
                when (state) {
                    is ComposeEditorState -> {
                        state.state.content.writeToSink(file.sink())
                    }

                    is SoraEditorState -> {
                        val content by state.state
                        file.writeText(content)
                    }
                }
                tab.markAsSaved()

                viewModelScope.launch(Dispatchers.Default) {
                    onSaveFile(tab.worktree, file)
                    EventBus.INSTANCE.post(FileSaveEvent(file, tab.worktree?.rootFile))
                }

                true
            } else false
        } else false
    }

    fun saveCurrentAs(newFile: KxFile): Boolean {
        val currentTabId = _state.value.activeTabId ?: return false
        return saveAs(currentTabId, newFile)
    }

    fun saveAs(tabId: TabId, newFile: KxFile): Boolean {
        val tab = _state.value.openTabs.find { it.id == tabId } ?: return false

        return if (tab is FileTab) {
            val editorState = tab.editorState

            try {
                when (editorState) {
                    is ComposeEditorState -> {
                        editorState.state.content.writeToSink(newFile.sink())
                    }

                    is SoraEditorState -> {
                        val text by editorState.state
                        newFile.writeText(text)
                    }
                }
            } catch (e: Exception) {
                notifier.error(e.message.orEmpty())
                return false
            }

            val newTab = FileTab(
                id = newFile.id,
                name = newFile.name,
                file = newFile,
                worktree = tab.worktree,
                editorState = editorState.copy()
            )

            replaceTab(tab.id, newTab)
            tab.markAsSaved()

            viewModelScope.launch(Dispatchers.Default) {
                onSaveFile(tab.worktree, newFile)
                EventBus.INSTANCE.post(FileSaveEvent(newFile, tab.worktree?.rootFile))
            }
            true
        } else false
    }

    fun saveAll(): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        _state.value.openTabs.forEach { tab ->
            if (tab is FileTab) {
                val file = tab.file
                val state = tab.editorState

                if (file.path != "untitled") {
                    val saved = try {
                        when (state) {
                            is ComposeEditorState -> {
                                state.state.content.writeToSink(file.sink())
                                true
                            }

                            is SoraEditorState -> {
                                val text by state.state
                                file.writeText(text)
                                true
                            }
                        }
                    } catch (e: Exception) {
                        notifier.error(e.message.orEmpty())
                        false
                    }

                    if (saved) {
                        tab.markAsSaved()

                        viewModelScope.launch(Dispatchers.Default) {
                            onSaveFile(tab.worktree, file)
                            EventBus.INSTANCE.post(FileSaveEvent(file, tab.worktree?.rootFile))
                        }
                    }
                    results[tab.name] = saved
                }
            }
        }

        return results
    }
}

internal expect suspend fun onCloseFileTab(worktree: Worktree?, file: KxFile)
internal expect suspend fun onSaveFile(worktree: Worktree?, file: KxFile)

fun EditorViewModel.openSettings() = openFile(Paths.settingsFile.toKxFile())

fun EditorViewModel.openDefaultSettings() {
    openFile(
        Paths.globalSettingsFile.toKxFile(),
        tabTitle = string(string.tab_title_default_settings),
        isInternal = true
    )
}

fun EditorViewModel.openExtensionScreen() {
    val id = "extension"

    if (isTabOpen(id)) setActiveTab(id) else {
        openTab(string(string.tab_title_extensions), id = id) {
            ExtensionScreen(modifier = Modifier.fillMaxSize())
        }
        setActiveTab(id)
    }
}

fun EditorViewModel.openUntitledFile() = openFile(KxFile("untitled"))

fun EditorViewModel.openLogViewer() {
    openTab("Logs") {
        LogViewerScreen(modifier = Modifier.fillMaxSize())
    }
}
