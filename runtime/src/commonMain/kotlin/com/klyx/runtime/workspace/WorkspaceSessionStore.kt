package com.klyx.runtime.workspace

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single
import org.koin.core.annotation.Singleton

fun createWorkspaceSessionDataStore(storage: Storage<WorkspaceSessionData>): DataStore<WorkspaceSessionData> =
    DataStoreFactory.create(storage = storage)

@Singleton
fun workspaceSessionStorage(): WorkspaceSessionStorage =
    WorkspaceSessionStorage(createWorkspaceSessionDataStore())

class WorkspaceSessionStorage internal constructor(
    internal val dataStore: DataStore<WorkspaceSessionData>,
)

@Single
class WorkspaceSessionStore(storage: WorkspaceSessionStorage) {
    private val dataStore = storage.dataStore

    suspend fun read(): WorkspaceSessionSnapshot? = dataStore.data.first().snapshot

    suspend fun save(snapshot: WorkspaceSessionSnapshot) {
        dataStore.updateData { it.copy(snapshot = snapshot) }
    }
}
