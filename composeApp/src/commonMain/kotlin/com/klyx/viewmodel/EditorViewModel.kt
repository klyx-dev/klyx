package com.klyx.viewmodel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.Environment
import com.klyx.core.Notifier
import com.klyx.core.event.EventBus
import com.klyx.core.event.io.FileCloseEvent
import com.klyx.core.event.io.FileOpenEvent
import com.klyx.core.event.io.FileSaveEvent
import com.klyx.core.file.FileId
import com.klyx.core.file.KxFile
import com.klyx.core.file.id
import com.klyx.core.file.isKlyxTempFile
import com.klyx.core.file.isPermissionRequired
import com.klyx.core.file.isValidUtf8
import com.klyx.core.file.okioSink
import com.klyx.core.file.sink
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.core.string
import com.klyx.editor.compose.CodeEditorState
import com.klyx.editor.compose.ExperimentalComposeCodeEditorApi
import com.klyx.editor.compose.text.buffer.writeToSink
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.parentAsWorktree
import com.klyx.ifNull
import com.klyx.res.Res.string
import com.klyx.res.tab_title_default_settings
import com.klyx.res.tab_title_extensions
import com.klyx.tab.Tab
import com.klyx.tab.TabId
import com.klyx.ui.component.WelcomeScreen
import com.klyx.ui.component.extension.ExtensionScreen
import com.klyx.ui.component.log.LogViewerScreen
import com.klyx.viewmodel.util.stateInWhileSubscribed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.use

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
        (tabState.openTabs.find { it is Tab.FileTab && it.id == tabState.activeTabId } as? Tab.FileTab)?.file
    }.stateInWhileSubscribed(initialValue = null)

    val activeTab = _state.map { tabState ->
        tabState.openTabs.find { it.id == tabState.activeTabId }
    }.stateInWhileSubscribed(null)

    val currentEditorState = _state.map { tabState ->
        (tabState.openTabs.find { it is Tab.FileTab && it.id == tabState.activeTabId } as? Tab.FileTab)?.editorState
    }.stateInWhileSubscribed(initialValue = null)

    val isTabOpen = _state.map {
        it.openTabs.isNotEmpty()
    }.stateInWhileSubscribed(initialValue = false)

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
        val tab = Tab.AnyTab(
            id = id.ifNull { name },
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
        worktree: Worktree? = file.parentAsWorktree(),
        tabTitle: String = file.name,
        isInternal: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val permissionRequired = file.isPermissionRequired(R_OK or W_OK)
            if (permissionRequired) {
                _state.update { it.copy(pendingFiles = it.pendingFiles + file) }
            }

            if (!file.isKlyxTempFile() && !permissionRequired && !file.isValidUtf8()) {
                notifier.error("(${file.name}) stream did not contain valid UTF-8")
                return@launch
            }

            val fileTab = Tab.FileTab(
                id = file.id,
                name = tabTitle,
                worktree = worktree,
                isInternal = isInternal,
                file = file,
                editorState = CodeEditorState(file)
            )

            withContext(Dispatchers.Main) {
                openTab(fileTab)
            }

            EventBus.instance.post(FileOpenEvent(file, worktree?.rootFile))
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
            if (tab is Tab.FileTab) {
                viewModelScope.launch(Dispatchers.Default) {
                    onCloseFileTab(tab.worktree, tab.file)
                    EventBus.instance.post(FileCloseEvent(tab.file, tab.worktree?.rootFile))
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
        return _state.value.openTabs.any { it is Tab.FileTab && it.id == fileId }
    }

    fun isFileTabActive(fileId: FileId): Boolean {
        return _state.value.activeTabId == fileId
    }

    fun isFileTab(tabId: TabId): Boolean {
        return _state.value.openTabs.any { it is Tab.FileTab && it.id == tabId }
    }

    fun isFileTabInternal(tabId: TabId): Boolean {
        return _state.value.openTabs.any { it is Tab.FileTab && it.isInternal && it.id == tabId }
    }

    fun saveCurrent(): Boolean {
        val current = _state.value
        val tab = current.openTabs.find { it.id == current.activeTabId } ?: return false

        return if (tab is Tab.FileTab) {
            val buffer = tab.editorState.buffer
            val file = tab.file

            if (file.path == "untitled") return false

            if (file.canWrite) {
                buffer.writeToSink(file.sink())
                tab.markAsSaved()

                viewModelScope.launch(Dispatchers.Default) {
                    onSaveFile(tab.worktree, file)
                    EventBus.instance.post(FileSaveEvent(file, tab.worktree?.rootFile))
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

        return if (tab is Tab.FileTab) {
            val editorState = tab.editorState

            try {
                editorState.buffer.writeToSink(newFile.sink())
            } catch (e: Exception) {
                notifier.error(e.message.orEmpty())
                return false
            }

            val newTab = Tab.FileTab(
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
                EventBus.instance.post(FileSaveEvent(newFile, tab.worktree?.rootFile))
            }
            true
        } else false
    }

    fun saveAll(): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        _state.value.openTabs.forEach { tab ->
            if (tab is Tab.FileTab) {
                val file = tab.file
                val buffer = tab.editorState.buffer

                if (file.path != "untitled") {
                    val saved = try {
                        buffer.readPiecesContent { content ->
                            file.okioSink().buffer().use { sink ->
                                sink.write(content.encodeToByteArray())
                            }
                        }
                        true
                    } catch (e: Exception) {
                        notifier.error(e.message.orEmpty())
                        false
                    }

                    if (saved) {
                        tab.markAsSaved()

                        viewModelScope.launch(Dispatchers.Default) {
                            onSaveFile(tab.worktree, file)
                            EventBus.instance.post(FileSaveEvent(file, tab.worktree?.rootFile))
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

fun EditorViewModel.openSettings() = openFile(KxFile(Environment.SettingsFilePath))

fun EditorViewModel.openDefaultSettings() {
    openFile(
        KxFile(Environment.InternalSettingsFilePath),
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

fun EditorViewModel.showWelcome() {
    openTab("Welcome") {
        WelcomeScreen(modifier = Modifier.fillMaxSize())
    }
}

fun EditorViewModel.openLogViewer() {
    openTab("Logs") {
        LogViewerScreen(modifier = Modifier.fillMaxSize())
    }
}
