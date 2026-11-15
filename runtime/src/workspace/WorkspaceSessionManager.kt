package com.klyx.runtime.workspace

import com.klyx.core.App
import com.klyx.core.Log
import com.klyx.runtime.settings.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class WorkspaceSessionManager(
    private val workspace: Workspace,
    private val settings: SettingsStore,
    private val sessions: WorkspaceSessionStore,
) {
    val error = MutableStateFlow<String?>(null)
    private val writes = Mutex()
    private val restored = CompletableDeferred<Unit>()

    suspend fun awaitRestored() = restored.await()

    fun start(): Job = App.global.backgroundScope.launch {
        try {
            restorePreviousSession()
            restored.complete(Unit)
            saveSessionChanges()
        } catch (error: Exception) {
            restored.completeExceptionally(error)
            if (error is CancellationException) ensureActive()
            this@WorkspaceSessionManager.error.value = "Could not restore or back up the workspace: ${error.message}"
            Log.e("WorkspaceSession", "Could not manage the workspace session", error)
        }
    }

    private suspend fun restorePreviousSession() {
        val saved = sessions.read() ?: return
        val snapshot = if (settings.read().restorePreviousSession) saved else saved.copy(
            groups = saved.groups.map { group -> group.copy(tabs = group.tabs.filter { it.buffer != null }) },
        )
        withContext(Dispatchers.Main) {
            workspace.restoreSession(snapshot)
        }
    }

    suspend fun saveCurrentSession() {
        awaitRestored()
        writes.withLock {
            val snapshot = withContext(Dispatchers.Main) { workspace.createSessionSnapshot() }
            sessions.save(snapshot)
            error.value = null
        }
    }

    private suspend fun saveSessionChanges() {
        workspace.observeSessionChanges()
            .collect {
                try {
                    saveCurrentSession()
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    error.value = "Recovery backup failed: ${failure.message}"
                }
            }
    }
}
