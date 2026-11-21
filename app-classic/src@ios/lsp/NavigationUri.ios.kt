package com.klyx.lsp

import io.github.vinceglb.filekit.PlatformFile
import platform.Foundation.NSURL

internal actual fun fileForNavigationUri(uri: String): PlatformFile? {
    return NSURL.URLWithString(uri)?.takeIf { it.scheme == "file" }?.let { PlatformFile(it) }
}

internal actual suspend fun resolveDefinitionAccess(uri: String, roots: List<PlatformFile>): PlatformFile? = null

internal actual fun termuxSnapshotSource(file: PlatformFile): PlatformFile? = null
