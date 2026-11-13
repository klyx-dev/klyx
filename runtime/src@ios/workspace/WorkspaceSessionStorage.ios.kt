package com.klyx.runtime.workspace

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

actual fun createWorkspaceSessionDataStore(): DataStore<WorkspaceSessionData> =
    createWorkspaceSessionDataStore(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = WorkspaceSessionSerializerIos,
            producePath = { App.global.workspaceSessionFilePath },
        ),
    )

private object WorkspaceSessionSerializerIos : OkioSerializer<WorkspaceSessionData> {
    override val defaultValue: WorkspaceSessionData = WorkspaceSessionData()

    override suspend fun readFrom(source: BufferedSource): WorkspaceSessionData =
        workspaceSessionJson.decodeFromString(WorkspaceSessionData.serializer(), source.readUtf8())

    override suspend fun writeTo(t: WorkspaceSessionData, sink: BufferedSink) {
        sink.writeUtf8(workspaceSessionJson.encodeToString(WorkspaceSessionData.serializer(), t))
    }
}

@OptIn(ExperimentalForeignApi::class)
actual val App.workspaceSessionFilePath: Path
    get() {
        val directory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return (requireNotNull(directory).path + "/$workspaceSessionFileName").toPath()
    }
