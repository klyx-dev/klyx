package com.klyx.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.klyx.util.applicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

typealias SettingsDataStore = DataStore<AppSettings>

suspend inline fun updateSettings(
    noinline transform: suspend AppSettings.() -> AppSettings
) = applicationContext().dataStore.updateData(transform)

suspend inline fun updateEditorSettings(
    noinline transform: suspend EditorSettings.() -> EditorSettings
) = updateSettings { copy(editor = transform(editor)) }.editor

suspend inline fun updateTerminalSettings(
    noinline transform: suspend TerminalSettings.() -> TerminalSettings
) = updateSettings { copy(terminal = transform(terminal)) }.terminal

suspend inline fun updateAppearanceSettings(
    noinline transform: suspend AppearanceSettings.() -> AppearanceSettings
) = updateSettings { copy(appearance = transform(appearance)) }.appearance

val Context.dataStore by dataStore(
    fileName = "settings.json",
    serializer = SettingsSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler {
        Log.e(
            "Klyx",
            "DataStore corruption detected in settings.json. Resetting to default values.",
            it
        )
        AppSettings()
    }
)

@OptIn(ExperimentalSerializationApi::class)
private object SettingsSerializer : Serializer<AppSettings> {

    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
        prettyPrintIndent = "  "
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
        allowComments = true
        explicitNulls = false
        isLenient = true
    }

    override val defaultValue = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            json.decodeFromStream(input)
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read App Settings", serialization)
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        json.encodeToStream(t, output)
    }
}
