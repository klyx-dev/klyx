package com.klyx.api.service

import com.klyx.api.data.preferences.AppSettings
import com.klyx.api.data.preferences.AppTheme
import com.klyx.api.data.preferences.AppearanceSettings
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.api.data.preferences.FileTreeSettings
import com.klyx.api.data.preferences.TerminalSettings
import com.klyx.api.plugin.PluginService
import kotlinx.coroutines.flow.Flow

interface Settings : PluginService {
    val settings: Flow<AppSettings>

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)

    suspend fun updateAppearanceSettings(transform: (AppearanceSettings) -> AppearanceSettings)

    suspend fun updateEditorSettings(transform: (EditorSettings) -> EditorSettings)

    suspend fun updateTerminalSettings(transform: (TerminalSettings) -> TerminalSettings)

    suspend fun updateFileTreeSettings(transform: (FileTreeSettings) -> FileTreeSettings)
}

suspend fun Settings.setTheme(theme: AppTheme) =
    updateAppearanceSettings {
        it.copy(theme = theme)
    }

suspend fun Settings.setDarkMode(enabled: Boolean) =
    updateAppearanceSettings {
        it.copy(theme = if (enabled) AppTheme.Dark else AppTheme.Light)
    }
