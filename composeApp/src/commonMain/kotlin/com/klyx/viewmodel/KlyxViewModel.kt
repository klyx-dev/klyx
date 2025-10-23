package com.klyx.viewmodel

import androidx.lifecycle.ViewModel
import com.klyx.core.Notifier
import com.klyx.extension.api.Project
import com.klyx.extension.api.Worktree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class KlyxMenuState(
    val showInfoDialog: Boolean = false,
    val showGiveFeedbackDialog: Boolean = false,
)

data class KlyxAppState(
    val showPermissionDialog: Boolean = false,
    val showLogViewer: Boolean = false,
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

    fun showInfoDialog() {
        _klyxMenuState.update { it.copy(showInfoDialog = true) }
    }

    fun showGiveFeedbackDialog() {
        _klyxMenuState.update { it.copy(showGiveFeedbackDialog = true) }
    }

    fun dismissGiveFeedbackDialog() {
        _klyxMenuState.update { it.copy(showGiveFeedbackDialog = false) }
    }

    fun dismissInfoDialog() {
        _klyxMenuState.update { it.copy(showInfoDialog = false) }
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

    fun showLogViewer() {
        _appState.update { it.copy(showLogViewer = true) }
    }

    fun dismissLogViewer() {
        _appState.update { it.copy(showLogViewer = false) }
    }
}
