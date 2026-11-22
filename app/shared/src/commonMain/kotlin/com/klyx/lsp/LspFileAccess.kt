package com.klyx.lsp

import com.klyx.platform.OperatingSystem
import com.klyx.platform.currentOperatingSystem
import io.github.vinceglb.filekit.PlatformFile

internal expect fun isAppPrivateFile(file: PlatformFile): Boolean

internal fun LspServerConfig.needsAccessibleFiles(
    operatingSystem: OperatingSystem = currentOperatingSystem,
): Boolean = operatingSystem == OperatingSystem.Android && transportType != LspTransportType.Stdio

internal fun LspServerConfig.canOpenPrivateFile(
    operatingSystem: OperatingSystem = currentOperatingSystem,
): Boolean = !needsAccessibleFiles(operatingSystem) || supportsContentOnly

internal fun LspServerConfig.canUseWorkspaceFolder(file: PlatformFile): Boolean =
    canUseWorkspaceFolder(isAppPrivateFile(file))

internal fun LspServerConfig.canUseWorkspaceFolder(
    isPrivate: Boolean,
    operatingSystem: OperatingSystem = currentOperatingSystem,
): Boolean = !needsAccessibleFiles(operatingSystem) || !isPrivate

// Android supplies canonical paths so aliases such as /data/data and /data/user/0 agree.
internal fun isPathInDirectories(path: String, roots: List<String>): Boolean =
    roots.any { root -> path == root.trimEnd('/') || path.startsWith(root.trimEnd('/') + "/") }

internal fun termuxRelativePath(root: String, target: String): String? {
    @Suppress("SdCardPath")
    fun normalize(path: String): String =
        path.replaceFirst(Regex("^/data/user/0/com[.]termux/"), "/data/data/com.termux/")

    val prefix = normalize(root).trimEnd('/') + "/"
    val path = normalize(target)
    if (!path.startsWith(prefix)) return null
    return path.removePrefix(prefix).takeIf { relative ->
        relative.split('/').none { it.isEmpty() || it == "." || it == ".." }
    }
}

/** The original server-side file for a read-only library snapshot, if any. */
internal expect fun termuxSnapshotSource(file: PlatformFile): PlatformFile?

@Suppress("SdCardPath")
internal const val termuxPrefix = "/data/data/com.termux/files/usr"
