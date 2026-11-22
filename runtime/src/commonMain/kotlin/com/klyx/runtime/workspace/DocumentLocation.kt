package com.klyx.runtime.workspace

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.io.files.Path

sealed interface DocumentLocation {
    data class Project(val projectId: ProjectId, val relativePath: Path) : DocumentLocation
    data class Platform(val file: PlatformFile) : DocumentLocation
}
