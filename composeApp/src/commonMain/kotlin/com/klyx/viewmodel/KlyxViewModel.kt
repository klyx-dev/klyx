package com.klyx.viewmodel

import androidx.lifecycle.ViewModel
import com.klyx.core.Notifier
import com.klyx.core.file.KxFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class KlyxMenuState(
    val showAboutDialog: Boolean = false
)

data class KlyxAppState(
    val showPermissionDialog: Boolean = false
)

class KlyxViewModel(
    private val notifier: Notifier
) : ViewModel() {
    private val _klyxMenuState = MutableStateFlow(KlyxMenuState())
    val klyxMenuState = _klyxMenuState.asStateFlow()

    private val _appState = MutableStateFlow(KlyxAppState())
    val appState = _appState.asStateFlow()

    private val _openProjects = MutableStateFlow(emptyList<KxFile>())
    val openProjects = _openProjects.asStateFlow()

    fun showAboutDialog() {
        _klyxMenuState.update { it.copy(showAboutDialog = true) }
    }

    fun dismissAboutDialog() {
        _klyxMenuState.update { it.copy(showAboutDialog = false) }
    }

    fun showPermissionDialog() {
        _appState.update { it.copy(showPermissionDialog = true) }
    }

    fun dismissPermissionDialog() {
        _appState.update { it.copy(showPermissionDialog = false) }
    }

    fun openProject(file: KxFile) {
        _openProjects.update { emptyList() }
        _openProjects.update { it + file }
    }

    fun closeProject() {
        _openProjects.update { emptyList() }
    }

    fun addFolderToProject(folder: KxFile) {
        _openProjects.update { it + folder }
    }
}
