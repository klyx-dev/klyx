package com.klyx.core.settings

import com.klyx.core.Environment
import com.klyx.core.event.EventBus
import com.klyx.core.event.SettingsChangeEvent
import com.klyx.core.file.KxFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName

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

    private val _settings = MutableStateFlow(AppSettings())
    val settings = _settings.asStateFlow()

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
                updateSettings { json.decodeFromString(text) }
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

    fun updateSettings(function: (AppSettings) -> AppSettings) {
        val oldSettings = settings.value.copy()
        _settings.update { function(settings.value) }
        EventBus.INSTANCE.postSync(SettingsChangeEvent(oldSettings, settings.value))
        save()
    }
}
