package com.klyx.core.settings

import androidx.compose.runtime.MutableState

actual object SettingsManager {
    actual var settings: MutableState<AppSettings>
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun load() {
    }

    actual fun save() {
    }

    actual fun updateSettings(settings: AppSettings) {
    }
}
