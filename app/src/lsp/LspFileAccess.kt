package com.klyx.lsp

import com.klyx.runtime.OperatingSystem
import com.klyx.runtime.currentOperatingSystem
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
