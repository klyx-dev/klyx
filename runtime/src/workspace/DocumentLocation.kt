package com.klyx.runtime.workspace

import okio.Path

sealed interface DocumentLocation {
    data class Project(val projectId: ProjectId, val relativePath: Path) : DocumentLocation
    data class Local(val path: Path) : DocumentLocation
    data class Uri(val value: String) : DocumentLocation
}
