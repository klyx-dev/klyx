package com.klyx.runtime.settings

import androidx.datastore.core.DataStore
import com.klyx.core.App
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Single
import org.koin.core.annotation.Singleton

@Singleton
fun settingsDataStore(): DataStore<Settings> = createDataStore()

@Single
class SettingsStore(private val store: DataStore<Settings>) {
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
}
