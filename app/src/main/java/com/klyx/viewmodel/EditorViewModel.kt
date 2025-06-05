package com.klyx.viewmodel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.file.FileId
import com.klyx.core.file.id
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

interface TabItem {
    val id: String
    val name: String
    val type: String
    val data: Any?
    val content: @Composable () -> Unit
}

val TabItem.isFileTab get() = type == "file" || type == "fileInternal"

data class EditorState(
    val openTabs: List<TabItem> = emptyList(),
    val activeTabId: String? = null
)

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    fun openFile(
        file: File,
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
            val fileTab = _state.value.openTabs.find { it.type == "file" && it.id == fileId.toString() } ?: return@launch
            val file = fileTab.data as? File ?: return@launch
            file.writeText(content)

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
                        }
                    } else {
                        it
                    }
                })
            }
        }
    }

    fun getActiveFile(): File? {
        val current = _state.value
        return (current.openTabs.find { it.id == current.activeTabId }?.data as? File)
    }
}
