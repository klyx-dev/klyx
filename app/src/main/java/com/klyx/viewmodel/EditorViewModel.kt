package com.klyx.viewmodel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.Klyx
import com.klyx.core.file.FileId
import com.klyx.core.file.FileWrapper
import com.klyx.editor.CodeEditorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TabItem(
    val id: String,
    val name: String,
    val type: String,
    val data: Any?,
    val content: @Composable () -> Unit,
    val editorState: CodeEditorState? = null
) {
    val isFileTab: Boolean get() = type == "file" || type == "fileInternal"
}

data class TabState(
    val openTabs: List<TabItem> = emptyList(),
    val activeTabId: String? = null
)

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(TabState())
    val state = _state.asStateFlow()

    fun openFile(
        file: FileWrapper,
        tabTitle: String = file.name,
        isInternal: Boolean = false
    ) {
        val fileTab = TabItem(
            id = file.id,
            name = tabTitle,
            type = if (isInternal) "fileInternal" else "file",
            data = file,
            content = {
                Box(modifier = Modifier.fillMaxSize())
            },
            editorState = CodeEditorState(initialText = file.readText(Klyx.application.applicationContext) ?: "")
        )

        _state.update { current ->
            if (current.openTabs.any { it.isFileTab && it.id == file.id }) {
                current.copy(activeTabId = file.id)
            } else {
                current.copy(
                    openTabs = current.openTabs + fileTab,
                    activeTabId = file.id
                )
            }
        }
    }

    fun openTab(
        type: String,
        id: String,
        name: String,
        content: @Composable () -> Unit,
        data: Any? = null
    ) {
        val tab = TabItem(
            id = id,
            name = name,
            type = type,
            data = data,
            content = content
        )

        _state.update { current ->
            if (current.openTabs.any { it.type == type && it.id == id }) {
                current.copy(activeTabId = id)
            } else {
                current.copy(
                    openTabs = current.openTabs + tab,
                    activeTabId = id
                )
            }
        }
    }

    fun closeTab(tabId: String) {
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

    fun setActiveTab(tabId: String) {
        _state.update { current ->
            if (current.openTabs.any { it.id == tabId }) {
                current.copy(activeTabId = tabId)
            } else {
                current
            }
        }
    }

    fun updateFileContent(fileId: FileId, content: String) {
        viewModelScope.launch {
            val fileTab = _state.value.openTabs.find { it.type == "file" && it.id == fileId } ?: return@launch
            val file = fileTab.data as? FileWrapper ?: return@launch
            file.write(Klyx.application.applicationContext, content)

            _state.update { current ->
                current.copy(openTabs = current.openTabs.map {
                    if (it.type == "file" && it.id == fileId) {
                        TabItem(
                            id = file.id,
                            name = file.name,
                            type = "file",
                            data = file,
                            content = {
                                Box(modifier = Modifier.fillMaxSize())
                            },
                            editorState = CodeEditorState(initialText = file.readText(Klyx.application.applicationContext) ?: "")
                        )
                    } else it
                })
            }
        }
    }

    fun saveCurrent(): Boolean {
        val current = _state.value
        val tab = current.openTabs.find { it.id == current.activeTabId } ?: return false
        val editorState = tab.editorState ?: return false
        val file = tab.data as? FileWrapper ?: return false
        val context = Klyx.application.applicationContext

        if (file.path == "untitled") {
            return false
        }

        return file.write(context, editorState.text).also { isSaved ->
            //if (isSaved) editorState.markAsSaved()
        }
    }

    fun saveAs(newFile: FileWrapper): Boolean {
        val current = _state.value
        val tab = current.openTabs.find { it.id == current.activeTabId } ?: return false
        val editorState = tab.editorState ?: return false
        val context = Klyx.application.applicationContext

        val saved = newFile.write(context, editorState.text)
        if (!saved) return false

        val newTab = TabItem(
            id = newFile.id,
            name = newFile.name,
            type = "file",
            data = newFile,
            content = {
                Box(modifier = Modifier.fillMaxSize())
            },
            editorState = CodeEditorState(initialText = editorState.text)
        )

        replaceTab(tab.id, newTab)
        //editorState.markAsSaved()
        return true
    }

    fun saveAll(): Map<String, Boolean> {
        val context = Klyx.application.applicationContext
        val results = mutableMapOf<String, Boolean>()

        _state.value.openTabs.forEach { tab ->
            if (tab.type == "file" || tab.type == "fileInternal") {
                val file = tab.data as? FileWrapper
                val editorState = tab.editorState
                
                if (file != null && editorState != null && file.path != "untitled") {
                    val saved = file.write(context, editorState.text)
                    if (saved) {
                        //editorState.markAsSaved()
                    }
                    results[tab.name] = saved
                }
            }
        }

        return results
    }

    fun getTab(tabId: String): TabItem? {
        return _state.value.openTabs.find { it.id == tabId }
    }

    fun getActiveFile(): FileWrapper? {
        val current = _state.value
        return (current.openTabs.find { it.id == current.activeTabId }?.data as? FileWrapper)
    }

    fun getActiveTab(): TabItem? {
        val current = _state.value
        return current.openTabs.find { it.id == current.activeTabId }
    }

    fun replaceTab(tabId: String, newTab: TabItem) {
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
}
