package com.klyx.core.settings

import androidx.compose.runtime.mutableStateOf
import com.klyx.core.Environment
import io.github.xn32.json5k.Json5
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object SettingsManager {
    private val json = Json5 {
        prettyPrint = true
        encodeDefaults = true
        indentationWidth = 2
    }

    actual var settings = mutableStateOf(AppSettings())

    private val settingsFile = File(Environment.SettingsFilePath)
    private val internalSettingsFile = File(Environment.InternalSettingsFilePath)

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
}
