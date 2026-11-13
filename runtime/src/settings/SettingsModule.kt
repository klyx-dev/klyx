package com.klyx.runtime.settings

import androidx.datastore.core.DataStore
import com.klyx.core.App
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Single
import org.koin.core.annotation.Singleton

@Singleton
fun settingsDataStore(): SettingsDataStore = SettingsDataStore(createDataStore())

class SettingsDataStore internal constructor(internal val dataStore: DataStore<Settings>)

@Single
class SettingsStore(storage: SettingsDataStore) {
    private val store = storage.dataStore

    val settings by lazy {
        store.data.stateIn(
            scope = App.global.backgroundScope,
            initialValue = Settings(),
            started = SharingStarted.WhileSubscribed(5000)
        )
    }

    suspend fun update(transform: suspend (Settings) -> Settings) {
        store.updateData(transform)
    }

    suspend fun read(): Settings = store.data.first()
}
