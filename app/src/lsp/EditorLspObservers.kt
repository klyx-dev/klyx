package com.klyx.lsp

import com.klyx.runtime.OperatingSystem
import com.klyx.runtime.currentOperatingSystem
import com.klyx.runtime.workspace.ProjectId
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

internal fun EditorLsp.observeWorkspaceProjects() {
    var previousProjectIds = emptySet<ProjectId>()
    scope.launch {
        workspace.projects.collect { projects ->
            val currentIds = projects.keys
            val added = (currentIds - previousProjectIds).mapNotNull { projects[it] }
            val removed = (previousProjectIds - currentIds)

            if (previousProjectIds.isNotEmpty() && (added.isNotEmpty() || removed.isNotEmpty())) {
                val addedFolders = added.map { WorkspaceFolder(uri = "file://${it.root}", name = it.root.name) }
                val removedFolders = removed.map { WorkspaceFolder(uri = "project://$it", name = it.value) }

                sessions.values.forEach { session ->
                    session.didChangeWorkspaceFolders(added = addedFolders, removed = removedFolders)
                }
            }
            previousProjectIds = currentIds
        }
    }
}

internal fun EditorLsp.observeInlayHintsSetting() {
    scope.launch {
        settingsStore.settings
            .map { it.showInlayHints }
            .distinctUntilChanged()
            .collect { show ->
                if (!show) {
                    inlayHintJobs.values.forEach { it.cancel() }
                    inlayHintJobs.clear()
                    inlayHintGeneration.clear()
                    withContext(Dispatchers.Main) {
                        editorStore.forEach { _, controller ->
                            controller.clearInlayHints()
                            controller.requestDecorationRefresh()
                        }
                    }
                }
            }
    }
}

internal fun EditorLsp.observeCustomServers() {
    scope.launch {
        settingsStore.settings
            .map { it.customLspServers }
            .distinctUntilChanged()
            .collect { servers -> LspServerRegistry.setCustomServers(servers.map { it.toConfig() }) }
    }
}

internal fun EditorLsp.observeServerAvailability() {
    scope.launch {
        settingsStore.settings
            .map { it.customLspServers }
            .distinctUntilChanged()
            .collect {
                refreshServerAvailability()
            }
    }
}

internal fun EditorLsp.refreshServerAvailability() {
    scope.launch {
        if (currentOperatingSystem != OperatingSystem.Android) return@launch
        val customs = settingsStore.settings.value.customLspServers.map { it.toConfig() }
        val commands = (LspServerRegistry.all() + customs).map { it.command }.distinct()
        val result = runCatching {
            withTimeoutOrNull(15.seconds) { TermuxBridge.probeTermuxBinaries(commands) }
        }.onFailure {
            if (it is CancellationException) {
                ensureActive()
            }
        }.getOrNull() ?: emptyMap()
        if (result.isNotEmpty()) {
            updateAvailability(result)
        }
    }
}
