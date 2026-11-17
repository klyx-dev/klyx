package com.klyx.document

import com.klyx.core.utils.relativeTo
import io.github.vinceglb.filekit.PlatformFile

internal fun isWorkspaceSessionFile(file: PlatformFile, sessionFile: PlatformFile): Boolean =
    runCatching { file.relativeTo(sessionFile).toString() == "." }.getOrDefault(false)
