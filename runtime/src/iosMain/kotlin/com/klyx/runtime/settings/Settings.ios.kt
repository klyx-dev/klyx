package com.klyx.runtime.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import com.klyx.core.App
import kotlinx.cinterop.ExperimentalForeignApi
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun createDataStore(): DataStore<Settings> = createDataStore(
    storage = OkioStorage(
        fileSystem = FileSystem.SYSTEM,
        serializer = SettingsSerializerIOS,
        producePath = { App.global.settingsFilePath }
    )
)

private object SettingsSerializerIOS : OkioSerializer<Settings> {
    override val defaultValue: Settings
        get() = Settings()

    override suspend fun readFrom(source: BufferedSource): Settings {
        return settingsJson.decodeFromString(Settings.serializer(), source.readUtf8())
    }

    override suspend fun writeTo(t: Settings, sink: BufferedSink) {
        sink.writeUtf8(settingsJson.encodeToString(Settings.serializer(),t))
    }
}

@OptIn(ExperimentalForeignApi::class)
actual val App.settingsFilePath: Path
    get() {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        return (requireNotNull(documentDirectory).path + "/$dataStoreFileName").toPath()
    }
