package com.klyx.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.Notifier
import com.klyx.core.file.FileId
import com.klyx.core.file.KxFile
import com.klyx.core.file.id
import com.klyx.core.generateId
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.ifNull
import com.klyx.tab.Tab
import com.klyx.tab.TabId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TabState(
    val openTabs: List<Tab> = emptyList(),
    val activeTabId: TabId? = null
)

@OptIn(ExperimentalCodeEditorApi::class)
@Suppress("MemberVisibilityCanBePrivate")
class EditorViewModel(
    private val notifier: Notifier
) : ViewModel() {
    private val _state = MutableStateFlow(TabState())
    val state = _state.asStateFlow()

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
        type: String? = null,
        id: TabId? = null,
        data: Any? = null,
        content: @Composable () -> Unit,
    ) {
        val tab = Tab.AnyTab(
            id = id.ifNull { "${type ?: "unknown"}_${generateId()}" },
            name = name,
            data = data,
            content = content
        )

        openTab(tab)
    }

    fun openFile(
        file: KxFile,
        tabTitle: String = file.name,
        isInternal: Boolean = false
    ) {
        println("[EditorViewModel] Open file path: ${file.path}")
        viewModelScope.launch(Dispatchers.IO) {
            val fileTab = Tab.FileTab(
                id = file.id,
                name = tabTitle,
                isInternal = isInternal,
                file = file,
                editorState = CodeEditorState(initialText = runCatching { file.readText() }.getOrElse { "" })
            )

            openTab(fileTab)
        }
    }

    fun closeTab(tabId: TabId) {
        _state.update { current ->
            val updatedTabs = current.openTabs.filterNot { it.id == tabId }
            val newActiveTabId = when {
                tabId == current.activeTabId -> updatedTabs.lastOrNull()?.id
                else -> current.activeTabId
            }

            current.copy(
                openTabs = updatedTabs,
                activeTabId = newActiveTabId
            )
        }
    }

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

    fun getActiveTab(): Tab? {
        val current = _state.value
        return current.openTabs.find { it.id == current.activeTabId }
    }

    fun getActiveFile(): KxFile? {
        val current = _state.value
        return (current.openTabs.find { it is Tab.FileTab && it.id == current.activeTabId } as? Tab.FileTab)?.file
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
            val text by tab.editorState
            val file = tab.file

            if (file.path == "untitled") return false

            if (file.canWrite) {
                file.writeText(text)
                tab.markAsSaved()
                true
            } else false
        } else false
    }

    fun saveAs(newFile: KxFile): Boolean {
        val current = _state.value
        val tab = current.openTabs.find { it.id == current.activeTabId } ?: return false

        return if (tab is Tab.FileTab) {
            val editorState = tab.editorState

            try {
                val text by editorState
                newFile.writeText(text)
            } catch (e: Exception) {
                return false
            }

            val newTab = Tab.FileTab(
                id = newFile.id,
                name = newFile.name,
                file = newFile,
                editorState = CodeEditorState(editorState)
            )

            replaceTab(tab.id, newTab)
            tab.markAsSaved()
            true
        } else false
    }

    fun saveAll(): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        _state.value.openTabs.forEach { tab ->
            if (tab is Tab.FileTab) {
                val file = tab.file
                val editorState = tab.editorState

                if (file.path != "untitled") {
                    val saved = try {
                        file.writeText(editorState.text)
                        true
                    } catch (e: Exception) {
                        false
                    }

                    if (saved) tab.markAsSaved()
                    results[tab.name] = saved
                }
            }
        }

        return results
    }
}
