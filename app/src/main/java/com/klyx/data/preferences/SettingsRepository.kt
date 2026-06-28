package com.klyx.data.preferences

import com.klyx.api.data.preferences.AppSettings
import com.klyx.api.data.preferences.AppTheme
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.api.data.preferences.FileTreeSettings
import com.klyx.api.data.preferences.TerminalSettings
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class SettingsRepository(private val dataStore: SettingsDataStore) {

    val settings = dataStore.data
    val appearanceSettings = settings.map { it.appearance }

    val appTheme = settings.map { it.appearance.theme }

    suspend fun updateSettings(transform: suspend (AppSettings) -> AppSettings) {
        dataStore.updateData(transform)
    }

    suspend fun updateTerminalSettings(block: suspend (TerminalSettings) -> TerminalSettings) {
        dataStore.updateData {
            it.copy(terminal = block(it.terminal))
        }
    }

    suspend fun updateEditorSettings(block: suspend (EditorSettings) -> EditorSettings) {
        dataStore.updateData {
            it.copy(editor = block(it.editor))
        }
    }

    suspend fun updateAppTheme(theme: AppTheme) = dataStore.updateData {
        it.copy(
            appearance = it.appearance.copy(theme = theme)
        )
    }

    suspend fun updateFileTreeSettings(block: (FileTreeSettings) -> FileTreeSettings) {
        dataStore.updateData {
            it.copy(fileTree = block(it.fileTree))
        }
    }
}
