package com.klyx.api.service

import com.klyx.api.data.preferences.AppSettings
import com.klyx.api.data.preferences.AppTheme
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.api.data.preferences.TerminalSettings
import com.klyx.core.Global
import kotlinx.coroutines.flow.Flow

interface SettingsService : Global {
    val settings: Flow<AppSettings>

    val isDarkMode: Flow<Boolean>
    val isAmoled: Flow<Boolean>
    val terminalFontSize: Flow<Float>
    val terminalCursorBlink: Flow<Boolean>
    val terminalScrollbackLines: Flow<Int>

    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setAmoledDarkMode(enabled: Boolean)
    suspend fun setTerminalFontSize(size: Float)
    suspend fun setTerminalCursorBlink(enabled: Boolean)

    suspend fun updateSettings(transform: suspend (AppSettings) -> AppSettings)
    suspend fun updateTerminalSettings(transform: suspend (TerminalSettings) -> TerminalSettings)
    suspend fun updateEditorSettings(transform: suspend (EditorSettings) -> EditorSettings)
    suspend fun updateAppTheme(theme: AppTheme)
}
