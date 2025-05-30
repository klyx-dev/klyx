package com.klyx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.file.FileId
import com.klyx.core.file.id
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class EditorState(
    val openFiles: List<File> = emptyList(),
    val activeFileId: FileId? = null
)

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    fun openFile(file: File) {
        _state.update { current ->
            if (current.openFiles.any { it.id == file.id }) {
                current.copy(activeFileId = file.id)
            } else {
                current.copy(
                    openFiles = current.openFiles + file,
                    activeFileId = file.id
                )
            }
        }
    }

    fun closeFile(fileId: FileId) {
        _state.update { current ->
            val updatedFiles = current.openFiles.filterNot { it.id == fileId }
            val newActiveFileId = when {
                fileId == current.activeFileId -> updatedFiles.lastOrNull()?.id
                else -> current.activeFileId
            }

            current.copy(
                openFiles = updatedFiles,
                activeFileId = newActiveFileId
            )
        }
    }

    fun setActiveFile(fileId: FileId) {
        _state.update { current ->
            if (current.openFiles.any { it.id == fileId }) {
                current.copy(activeFileId = fileId)
            } else {
                current
            }
        }
    }

    fun updateFileContent(fileId: FileId, content: String) {
        viewModelScope.launch {
            val file = _state.value.openFiles.find { it.id == fileId } ?: return@launch
            file.writeText(content)

            _state.update { current ->
                current.copy(openFiles = current.openFiles.map {
                    if (it.id == fileId) {
                        file
                    } else {
                        it
                    }
                })
            }
        }
    }

    fun getActiveFile(): File? {
        val current = _state.value
        return current.openFiles.find { it.id == current.activeFileId }
    }
}
