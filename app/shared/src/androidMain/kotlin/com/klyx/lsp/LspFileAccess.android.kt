package com.klyx.lsp

import com.klyx.core.App
import com.klyx.core.applicationContext
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import java.io.File

internal actual fun isAppPrivateFile(file: PlatformFile): Boolean {
    val context = App.global.applicationContext
    val roots = listOf(context.dataDir, context.createDeviceProtectedStorageContext().dataDir)
    return isPathInDirectories(File(file.path).canonicalPath, roots.map { it.canonicalPath })
}
