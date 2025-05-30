package com.klyx.core.settings

import android.content.Context
import androidx.compose.runtime.mutableStateOf
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

    val settingsFile: (Context) -> File = { context -> File(context.getExternalFilesDir(null), FILE_NAME) }

    fun load(context: Context) {
        val file = settingsFile(context)
        if (file.exists()) {
            runCatching {
                val text = file.readText()
                settings.value = json.decodeFromString(text)
                save(context)
            }.onFailure { save(context) }
        } else {
            save(context)
        }
    }

    fun save(context: Context) {
        runCatching {
            val file = settingsFile(context)
            file.delete()
            file.writeText(json.encodeToString(settings.value))
        }
    }

    fun updateSettings(context: Context, settings: AppSettings) {
        this.settings.value = settings
        save(context)
    }
}
