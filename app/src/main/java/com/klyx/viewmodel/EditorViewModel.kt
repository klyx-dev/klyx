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
import com.klyx.editor.compose.EditorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface TabItem {
    val id: String
    val name: String
    val type: String
    val data: Any?
    val content: @Composable () -> Unit
    val editorState: EditorState? get() = null
}

val TabItem.isFileTab get() = type == "file" || type == "fileInternal"

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
        val fileTab = object : TabItem {
            override val id: String = file.id
            override val name: String = tabTitle
            override val type: String = if (isInternal) "fileInternal" else "file"
            override val data: Any = file
            override val content: @Composable () -> Unit = {
                Box(modifier = Modifier.fillMaxSize())
            }
            override val editorState: EditorState
                get() = EditorState(initialText = file.readText(Klyx.application.applicationContext) ?: "")
        }

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
        val tab = object : TabItem {
            override val id: String = id
            override val name: String = name
            override val type: String = type
            override val data: Any? = data
            override val content: @Composable () -> Unit = content
        }

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
                        object : TabItem {
                            override val id: String = file.id
                            override val name: String = file.name
                            override val type: String = "file"
                            override val data: Any = file
                            override val content: @Composable () -> Unit = {
                                // File content will be handled by EditorScreen
                                Box(modifier = Modifier.fillMaxSize())
                            }
                            override val editorState: EditorState
                                get() = EditorState(initialText = file.readText(Klyx.application.applicationContext) ?: "")
                        }
                    } else {
                        it
                    }
                })
            }
        }
    }

    fun saveCurrent() {
        val current = _state.value
        val tab = current.openTabs.find { it.id == current.activeTabId } ?: return
        val editorState = tab.editorState ?: return
        val file = tab.data as? FileWrapper ?: return
        val context = Klyx.application.applicationContext

        file.write(context, editorState.text)
        editorState.markAsSaved()
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
}
