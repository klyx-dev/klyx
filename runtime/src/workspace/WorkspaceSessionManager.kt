package com.klyx.runtime.workspace

import com.klyx.core.App
import com.klyx.core.Log
import com.klyx.runtime.settings.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class WorkspaceSessionManager(
    private val workspace: Workspace,
    private val settings: SettingsStore,
    private val sessions: WorkspaceSessionStore,
) {
    fun start() {
        App.global.backgroundScope.launch {
            try {
                restorePreviousSession()
                saveCurrentSession()
                saveSessionChanges()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("WorkspaceSession", "Could not manage the workspace session", error)
            }
        }
    }

    private suspend fun restorePreviousSession() {
        if (!settings.read().restorePreviousSession) return

        val snapshot = sessions.read() ?: return
        withContext(Dispatchers.Main) {
            workspace.restoreSession(snapshot)
        }
    }

    private suspend fun saveCurrentSession() {
        sessions.save(workspace.createSessionSnapshot())
    }

    @OptIn(FlowPreview::class)
    private suspend fun saveSessionChanges() {
        workspace.observeSessionChanges()
            .debounce(300)
            .collect { snapshot -> sessions.save(snapshot) }
    }
}
