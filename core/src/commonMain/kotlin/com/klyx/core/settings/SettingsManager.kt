@file:OptIn(ExperimentalContracts::class)

package com.klyx.core.settings

import androidx.compose.runtime.MutableState
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

const val SETTINGS_FILE_NAME = "settings.json"

expect object SettingsManager {
    var settings: MutableState<AppSettings>
    val defaultSettings: AppSettings

    fun load()
    fun save()
    fun updateSettings(settings: AppSettings)
}

inline fun AppSettings.update(function: (AppSettings) -> AppSettings) {
    contract { callsInPlace(function, InvocationKind.EXACTLY_ONCE) }
    SettingsManager.updateSettings(function(this))
}

inline fun EditorSettings.update(function: (EditorSettings) -> EditorSettings) {
    contract { callsInPlace(function, InvocationKind.EXACTLY_ONCE) }
    SettingsManager.settings.value.update { it.copy(editor = function(this)) }
}
