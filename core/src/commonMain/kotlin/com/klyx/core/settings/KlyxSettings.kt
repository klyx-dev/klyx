package com.klyx.core.settings

import androidx.compose.runtime.Stable
import kotlin.jvm.JvmName

@Stable
sealed interface KlyxSettings

@JvmName("updateSettings")
inline fun <Settings : KlyxSettings> Settings.update(
    crossinline function: (Settings) -> Settings
) {
    SettingsManager.updateSettings {
        when (val settings = function(this)) {
            is AppSettings -> settings
            is EditorSettings -> it.copy(editor = settings)
            is StatusBarSettings -> it.copy(statusBar = settings)
        }
    }
}
