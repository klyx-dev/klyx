package com.klyx.runtime.workspace

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

actual fun createWorkspaceSessionDataStore(): DataStore<WorkspaceSessionData> =
    createWorkspaceSessionDataStore(
        storage = FileStorage(
            serializer = WorkspaceSessionSerializerAndroid,
            produceFile = { App.global.workspaceSessionFilePath.toFile() },
        ),
    )

@OptIn(ExperimentalSerializationApi::class)
private object WorkspaceSessionSerializerAndroid : Serializer<WorkspaceSessionData> {
    override val defaultValue: WorkspaceSessionData = WorkspaceSessionData()

    override suspend fun readFrom(input: InputStream): WorkspaceSessionData =
        workspaceSessionJson.decodeFromStream(WorkspaceSessionData.serializer(), input)

    override suspend fun writeTo(t: WorkspaceSessionData, output: OutputStream) {
        workspaceSessionJson.encodeToStream(WorkspaceSessionData.serializer(), t, output)
    }
}

actual val App.workspaceSessionFilePath: Path
    get() = applicationContext.filesDir.resolve(workspaceSessionFileName).toOkioPath()
