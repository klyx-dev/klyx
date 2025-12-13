package com.klyx.core.settings

import com.klyx.core.backgroundScope
import com.klyx.core.event.EventBus
import com.klyx.core.event.SettingsChangeEvent
import com.klyx.core.io.Paths
import com.klyx.core.io.fs
import com.klyx.core.io.globalSettingsFile
import com.klyx.core.io.settingsFile
import com.klyx.core.logging.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

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

    private inline val settingsFile get() = Paths.settingsFile
    private inline val globalSettingsFile get() = Paths.globalSettingsFile

    private val _settings = MutableStateFlow(AppSettings())
    val settings = _settings.asStateFlow()

    val defaultSettings: AppSettings by lazy {
        fs.source(globalSettingsFile).buffered().use {
            json.decodeFromString(it.readString())
        }
    }

    init {
        backgroundScope.launch(Dispatchers.IO) {
            runCatching {
                fs.delete(globalSettingsFile, mustExist = false)
                fs.sink(globalSettingsFile).buffered().use {
                    it.writeString(json.encodeToString(settings.value))
                }
            }.onFailure {
                it.printStackTrace()
                log.error { "Failed to create global settings file: $globalSettingsFile" }
                throw IllegalStateException("Failed to create internal settings file", it)
            }
        }
    }

    suspend fun load() = withContext(Dispatchers.IO) {
        if (fs.exists(settingsFile)) {
            runCatching {
                val text = fs.source(settingsFile).buffered().use { it.readString() }
                updateSettings { json.decodeFromString(text) }
            }.onFailure { save() }
        } else {
            save()
        }
    }

    fun save() {
        runCatching {
            fs.delete(settingsFile, mustExist = false)
            fs.sink(settingsFile).buffered().use {
                it.writeString(json.encodeToString(settings.value))
            }
        }.onFailure { it.printStackTrace() }
    }

    fun updateSettings(function: (AppSettings) -> AppSettings) {
        val oldSettings = settings.value.copy()
        _settings.update { function(settings.value) }
        EventBus.INSTANCE.postSync(SettingsChangeEvent(oldSettings, settings.value))
        save()
    }
}
