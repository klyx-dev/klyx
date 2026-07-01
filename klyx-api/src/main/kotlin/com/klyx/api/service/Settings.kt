package com.klyx.api.service

import com.klyx.api.data.preferences.AppSettings
import com.klyx.api.data.preferences.AppTheme
import com.klyx.api.data.preferences.AppearanceSettings
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.api.data.preferences.FileTreeSettings
import com.klyx.api.data.preferences.TerminalSettings
import com.klyx.api.plugin.PluginService
import kotlinx.coroutines.flow.Flow

/**
 * Service for managing application-wide settings and preferences.
 *
 * This service provides a reactive stream of the current [AppSettings] and methods
 * to update various sub-settings like appearance, editor, terminal, and file tree configurations.
 *
 * ### Example
 * ```kotlin
 * val settings: Settings by plugin()
 *
 * // Observe settings
 * settings.settings.collect { current ->
 *     println("Theme changed to: ${current.appearance.theme}")
 * }
 *
 * // Update settings
 * settings.setDarkMode(true)
 * ```
 */
interface Settings : PluginService {

    /**
     * A [Flow] of the current [AppSettings].
     *
     * Subscribing to this flow allows you to react to setting changes in real-time.
     */
    val settings: Flow<AppSettings>

    /**
     * Updates the root [AppSettings] using the provided [transform].
     */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)

    /**
     * Updates only the [AppearanceSettings] sub-section.
     */
    suspend fun updateAppearanceSettings(transform: (AppearanceSettings) -> AppearanceSettings)

    /**
     * Updates only the [EditorSettings] sub-section.
     */
    suspend fun updateEditorSettings(transform: (EditorSettings) -> EditorSettings)

    /**
     * Updates only the [TerminalSettings] sub-section.
     */
    suspend fun updateTerminalSettings(transform: (TerminalSettings) -> TerminalSettings)

    /**
     * Updates only the [FileTreeSettings] sub-section.
     */
    suspend fun updateFileTreeSettings(transform: (FileTreeSettings) -> FileTreeSettings)
}

/**
 * Convenience extension to set the application theme.
 */
suspend fun Settings.setTheme(theme: AppTheme) =
    updateAppearanceSettings {
        it.copy(theme = theme)
    }

/**
 * Convenience extension to toggle dark mode.
 */
suspend fun Settings.setDarkMode(enabled: Boolean) =
    updateAppearanceSettings {
        it.copy(theme = if (enabled) AppTheme.Dark else AppTheme.Light)
    }
