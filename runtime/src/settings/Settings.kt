@file:OptIn(ExperimentalSerializationApi::class)

package com.klyx.runtime.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import com.klyx.core.App
import com.klyx.runtime.Platform
import com.klyx.runtime.currentPlatform
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okio.Path

@Serializable
data class Settings(
    val showAllProjectsTabs: Boolean = false,
    val showUndoRedoButtons: Boolean = currentPlatform != Platform.JVM,
    val showInlayHints: Boolean = true,
    val autoSave: Boolean = false,
    val roundedUi: Boolean = false,
    val cornerRadius: Int = 8,
    val customLspServers: List<CustomLspServer> = emptyList(),
)

/**
 * A user-defined language server, configured from Settings.
 *
 * When [manualSocket] is false the server is launched automatically
 * (on Android via a Termux bridge, on desktop as a local stdio process).
 * When true the app instead connects to `127.0.0.1:[port]` and the user
 * runs a socat listener inside Termux themselves.
 */
@Serializable
data class CustomLspServer(
    val languageId: String,
    val extensions: List<String> = emptyList(),
    val command: String = "",
    val args: List<String> = emptyList(),
    val port: Int = 0,
    val manualSocket: Boolean = false,
)

fun createDataStore(storage: Storage<Settings>): DataStore<Settings> =
    DataStoreFactory.create(storage = storage)

expect fun createDataStore(): DataStore<Settings>

expect val App.settingsFilePath: Path

internal const val dataStoreFileName = "settings.json"

internal val settingsJson = Json {
    encodeDefaults = true
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    allowComments = true
    explicitNulls = false
    isLenient = true
}
