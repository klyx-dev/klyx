package com.klyx.core.settings

import androidx.compose.runtime.mutableStateOf
import com.klyx.core.Env
import io.github.xn32.json5k.Json5
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

object SettingsManager {
    private val json = Json5 {
        prettyPrint = true
        encodeDefaults = true
        indentationWidth = 2
    }

    private const val FILE_NAME = "settings.json"

    var settings = mutableStateOf(AppSettings())
        private set

    val settingsFile = File(Env.APP_HOME_DIR, FILE_NAME)
    val internalSettingsFile = File(Env.INTERNAL_APP_HOME_DIR, FILE_NAME)

    init {
        runCatching {
            internalSettingsFile.delete()
            internalSettingsFile.writeText(json.encodeToString(settings.value))
        }.onFailure {
            throw IllegalStateException("Failed to create internal settings file", it)
        }
    }

    fun load() {
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
