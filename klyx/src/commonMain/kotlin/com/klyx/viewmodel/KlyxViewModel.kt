package com.klyx.viewmodel

import androidx.lifecycle.ViewModel
import com.klyx.project.Project
import com.klyx.project.Worktree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class KlyxMenuState(
    val showInfoDialog: Boolean = false,
    val showGiveFeedbackDialog: Boolean = false,
)

data class KlyxAppState(
    val showPermissionDialog: Boolean = false,
    val showLogViewer: Boolean = false,
)

class KlyxViewModel : ViewModel() {

    val klyxMenuState: StateFlow<KlyxMenuState>
        field = MutableStateFlow(KlyxMenuState())

    val appState: StateFlow<KlyxAppState>
        field = MutableStateFlow(KlyxAppState())

    val openedProject: StateFlow<Project>
        field = MutableStateFlow(Project(emptyList()))

    fun showInfoDialog() {
        klyxMenuState.update { it.copy(showInfoDialog = true) }
    }

    fun showGiveFeedbackDialog() {
        klyxMenuState.update { it.copy(showGiveFeedbackDialog = true) }
    }

    fun dismissGiveFeedbackDialog() {
        klyxMenuState.update { it.copy(showGiveFeedbackDialog = false) }
    }

    fun dismissInfoDialog() {
        klyxMenuState.update { it.copy(showInfoDialog = false) }
    }

    fun showPermissionDialog() {
        appState.update { it.copy(showPermissionDialog = true) }
    }

    fun dismissPermissionDialog() {
        appState.update { it.copy(showPermissionDialog = false) }
    }

    fun openProject(worktree: Worktree) {
        openedProject.update { Project(listOf(worktree)) }
    }

    fun openProject(project: Project) {
        openedProject.update { project }
    }

    fun closeProject() {
        openedProject.update { Project(emptyList()) }
    }

    fun addWorktreeToProject(worktree: Worktree) {
        openedProject.update { it.copy(worktrees = it.worktrees + worktree) }
    }

    fun showLogViewer() {
        appState.update { it.copy(showLogViewer = true) }
    }

    fun dismissLogViewer() {
        appState.update { it.copy(showLogViewer = false) }
    }
}
