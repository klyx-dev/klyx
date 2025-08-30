package com.klyx.core.settings

import androidx.compose.runtime.MutableState

const val SETTINGS_FILE_NAME = "settings.json"

expect object SettingsManager {
    var settings: MutableState<AppSettings>
    val defaultSettings: AppSettings

    fun load()
    fun save()
    fun updateSettings(settings: AppSettings)
}
