@file:OptIn(ExperimentalContracts::class)

package com.klyx.core.settings

import androidx.compose.runtime.mutableStateOf
import com.klyx.core.Environment
import com.klyx.core.file.KxFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

const val SETTINGS_FILE_NAME = "settings.json"

object SettingsManager {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrintIndent = "  "
        prettyPrint = true
        namingStrategy = JsonNamingStrategy.SnakeCase
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
        allowComments = true
    }

    private val settingsFile = KxFile(Environment.SettingsFilePath)
    private val internalSettingsFile = KxFile(Environment.InternalSettingsFilePath)

    var settings = mutableStateOf(AppSettings())

    val defaultSettings: AppSettings
        get() = json.decodeFromString(internalSettingsFile.readText())

    init {
        runCatching {
            internalSettingsFile.delete()
            internalSettingsFile.writeText(json.encodeToString(settings.value))
        }.onFailure {
            it.printStackTrace()
            println(internalSettingsFile.absolutePath)
            throw IllegalStateException("Failed to create internal settings file", it)
        }
    }

    fun load() {
        if (settingsFile.exists) {
            runCatching {
                val text = settingsFile.readText()
                settings.value = json.decodeFromString(text)
                save()
            }.onFailure { save() }
        } else {
            save()
        }
    }

    fun save() {
        runCatching {
            settingsFile.delete()
            settingsFile.writeText(json.encodeToString(settings.value))
        }.onFailure { it.printStackTrace() }
    }

    fun updateSettings(settings: AppSettings) {
        this.settings.value = settings
        save()
    }
}

inline fun AppSettings.update(function: (AppSettings) -> AppSettings) {
    contract { callsInPlace(function, InvocationKind.EXACTLY_ONCE) }
    SettingsManager.updateSettings(function(this))
}

inline fun EditorSettings.update(function: (EditorSettings) -> EditorSettings) {
    contract { callsInPlace(function, InvocationKind.EXACTLY_ONCE) }
    SettingsManager.settings.value.update { it.copy(editor = function(this)) }
}
