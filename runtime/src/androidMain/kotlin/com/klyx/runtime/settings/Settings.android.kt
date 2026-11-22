package com.klyx.runtime.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.core.Serializer
import com.klyx.core.App
import com.klyx.core.applicationContext
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okio.Path
import okio.Path.Companion.toOkioPath

actual fun createDataStore(): DataStore<Settings> = createDataStore(
    storage = FileStorage(
        serializer = SettingsSerializerAndroid,
        produceFile = { App.global.settingsFilePath.toFile() }
    )
)

@OptIn(ExperimentalSerializationApi::class)
private object SettingsSerializerAndroid : Serializer<Settings> {

    override val defaultValue: Settings get() = Settings()

    override suspend fun readFrom(input: InputStream): Settings {
        return settingsJson.decodeFromStream(Settings.serializer(), input)
    }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        settingsJson.encodeToStream(Settings.serializer(),t, output)
    }
}

actual val App.settingsFilePath: Path
    get() = applicationContext.filesDir.resolve(dataStoreFileName).toOkioPath()
