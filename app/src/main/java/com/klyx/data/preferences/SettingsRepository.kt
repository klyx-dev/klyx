package com.klyx.data.preferences

import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class SettingsRepository(private val dataStore: SettingsDataStore) {

    val settings = dataStore.data
    val appearanceSettings = settings.map { it.appearance }

    val appTheme = settings.map { it.appearance.theme }

    suspend fun updateTerminalSettings(block: (TerminalSettings) -> TerminalSettings) {
        dataStore.updateData {
            it.copy(terminal = block(it.terminal))
        }
    }

    suspend fun updateAppTheme(theme: AppTheme) = dataStore.updateData {
        it.copy(
            appearance = it.appearance.copy(theme = theme)
        )
    }
}
