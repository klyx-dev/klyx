package com.klyx.terminal

import android.content.Context
import com.klyx.core.terminal.sandboxDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SetupNextStage {
    None, Extraction
}

context(context: Context)
suspend fun getNextStage() = withContext(Dispatchers.IO) {
    val sandboxFile = context.cacheDir.resolve("sandbox.tar.gz")
    val rootfsFiles = sandboxDir.listFiles()?.filter {
        it.absolutePath != sandboxDir.resolve("tmp").absolutePath
    }.orEmpty()

    if (sandboxFile.exists() || rootfsFiles.isEmpty()) SetupNextStage.Extraction else SetupNextStage.None
}
