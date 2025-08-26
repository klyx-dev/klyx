package com.klyx.viewmodel

import androidx.lifecycle.ViewModel
import com.klyx.core.Notifier
import com.klyx.extension.api.Project
import com.klyx.extension.api.Worktree
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

    private val _openedProject = MutableStateFlow(Project(emptyList()))
    val openedProject = _openedProject.asStateFlow()

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

    fun openProject(worktree: Worktree) {
        _openedProject.update { Project(listOf(worktree.id)) }
    }

    fun closeProject() {
        _openedProject.update { Project(emptyList()) }
    }

    fun addWorktreeToProject(worktree: Worktree) {
        _openedProject.update { it.copy(worktreeIds = it.worktreeIds + worktree.id) }
    }
}
