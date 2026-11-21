package com.klyx.lsp

import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import java.net.URI

internal actual fun fileForNavigationUri(uri: String): PlatformFile? {
    return runCatching { PlatformFile(File(URI(uri))) }.getOrNull()
}

internal actual suspend fun resolveDefinitionAccess(uri: String, roots: List<PlatformFile>): PlatformFile? = null

internal actual fun termuxSnapshotSource(file: PlatformFile): PlatformFile? = null
