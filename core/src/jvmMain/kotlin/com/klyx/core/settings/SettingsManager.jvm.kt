package com.klyx.core.settings

import androidx.compose.runtime.mutableStateOf
import com.klyx.core.Environment
import io.github.xn32.json5k.Json5
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.io.File

actual object SettingsManager {
    private val json5 = Json5 {
        prettyPrint = true
        encodeDefaults = true
        indentationWidth = 2
    }

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

    actual var settings = mutableStateOf(AppSettings())

    private val settingsFile = File(Environment.SettingsFilePath)
    private val internalSettingsFile = File(Environment.InternalSettingsFilePath)

    init {
        runCatching {
            internalSettingsFile.parentFile?.mkdirs()
            internalSettingsFile.delete()
            internalSettingsFile.writeText(json.encodeToString(settings.value))
        }.onFailure {
            it.printStackTrace()
            println(internalSettingsFile.absolutePath)
            throw IllegalStateException("Failed to create internal settings file", it)
        }
    }

    actual fun load() {
        if (settingsFile.exists()) {
            runCatching {
                val text = settingsFile.readText()
                settings.value = json.decodeFromString(text)
                save()
            }.onFailure { save() }
        } else {
            save()
        }
    }

    actual fun save() {
        runCatching {
            settingsFile.delete()
            settingsFile.writeText(json.encodeToString(settings.value))
        }.onFailure { it.printStackTrace() }
    }

    actual fun updateSettings(settings: AppSettings) {
        this.settings.value = settings
        save()
    }

    actual val defaultSettings: AppSettings
        get() = settings.value
}
