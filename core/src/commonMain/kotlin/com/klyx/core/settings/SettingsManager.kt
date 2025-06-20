package com.klyx.core.settings

import androidx.compose.runtime.MutableState

const val SETTINGS_FILE_NAME = "settings.json"

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object SettingsManager {
    var settings: MutableState<AppSettings>

    fun load()
    fun save()
    fun updateSettings(settings: AppSettings)
}
