package com.klyx.service

import com.klyx.api.data.preferences.AppSettings
import com.klyx.api.data.preferences.AppTheme
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.api.data.preferences.TerminalSettings
import com.klyx.api.service.FontService
import com.klyx.api.service.SettingsService
import com.klyx.api.service.TabService
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.data.preferences.FontManager
import com.klyx.data.preferences.SettingsRepository
import com.klyx.presentation.viewmodel.EditorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsServiceWrapper(
    private val repo: SettingsRepository
) : SettingsService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val settings: Flow<AppSettings> = repo.settings

    override val isDarkMode: Flow<Boolean> = repo.appTheme.map { theme ->
        when (theme) {
            AppTheme.Dark -> true
            AppTheme.Light -> false
            AppTheme.System -> false
        }
    }

    override val isAmoled: Flow<Boolean> = repo.appearanceSettings.map { it.amoledDarkMode }

    override val terminalFontSize: Flow<Float> = repo.settings.map { it.terminal.fontSize }

    override val terminalCursorBlink: Flow<Boolean> = repo.settings.map { it.terminal.cursorBlink }

    override val terminalScrollbackLines: Flow<Int> = repo.settings.map { it.terminal.scrollbackLines }

    override suspend fun setDarkMode(enabled: Boolean) {
        repo.updateAppTheme(
            if (enabled) AppTheme.Dark
            else AppTheme.Light
        )
    }

    override suspend fun setAmoledDarkMode(enabled: Boolean) {
        repo.updateAppTheme(
            if (enabled) AppTheme.Dark
            else AppTheme.System
        )
    }

    override suspend fun setTerminalFontSize(size: Float) {
        repo.updateTerminalSettings { it.copy(fontSize = size) }
    }

    override suspend fun setTerminalCursorBlink(enabled: Boolean) {
        repo.updateTerminalSettings { it.copy(cursorBlink = enabled) }
    }

    override suspend fun updateSettings(transform: suspend (AppSettings) -> AppSettings) {
        repo.updateSettings(transform)
    }

    override suspend fun updateTerminalSettings(transform: suspend (TerminalSettings) -> TerminalSettings) {
        repo.updateTerminalSettings(transform)
    }

    override suspend fun updateEditorSettings(transform: suspend (EditorSettings) -> EditorSettings) {
        repo.updateEditorSettings(transform)
    }

    override suspend fun updateAppTheme(theme: AppTheme) {
        repo.updateAppTheme(theme)
    }
}

class FontServiceWrapper(
    private val manager: FontManager
) : FontService {
    override suspend fun getFontFamily(uri: String?) = manager.getFontFamily(uri)
    override fun clearCache() { manager.clearCache() }
}

class TabServiceWrapper(
    private val editorViewModel: EditorViewModel
) : TabService {
    override fun openTab(tab: WorkspaceTab) = editorViewModel.openTab(tab)
    override fun closeTab(tabId: String) = editorViewModel.closeTab(tabId)
}


