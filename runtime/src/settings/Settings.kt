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
