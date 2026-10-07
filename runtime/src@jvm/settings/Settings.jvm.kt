package com.klyx.runtime.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.core.Serializer
import com.klyx.core.App
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okio.Path
import okio.Path.Companion.toOkioPath

actual fun createDataStore(): DataStore<Settings> = createDataStore(
    storage = FileStorage(
        serializer = SettingsSerializerJvm,
        produceFile = { settingsDirectory().resolve(dataStoreFileName) }
    )
)

@OptIn(ExperimentalSerializationApi::class)
private object SettingsSerializerJvm : Serializer<Settings> {

    override val defaultValue: Settings get() = Settings()

    override suspend fun readFrom(input: InputStream): Settings {
        return settingsJson.decodeFromStream(Settings.serializer(), input)
    }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        settingsJson.encodeToStream(Settings.serializer(),t, output)
    }
}

fun settingsDirectory(): File {
    val os = System.getProperty("os.name").lowercase()

    return when {
        os.contains("windows") -> {
            File(
                System.getenv("APPDATA")
                    ?: File(
                        System.getProperty("user.home"),
                        "AppData/Roaming"
                    ).path,
                App.NAME,
            )
        }

        os.contains("mac") || os.contains("darwin") -> {
            File(
                System.getProperty("user.home"),
                "Library/Application Support/${App.NAME}",
            )
        }

        else -> {
            val xdgConfigHome = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }

            File(
                xdgConfigHome ?: File(System.getProperty("user.home"), ".config").path,
                App.NAME,
            )
        }
    }
}

actual val App.settingsFilePath: Path
    get() = settingsDirectory().resolve(dataStoreFileName).toOkioPath()
