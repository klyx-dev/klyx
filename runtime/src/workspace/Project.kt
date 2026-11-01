package com.klyx.runtime.workspace

import kotlinx.io.files.Path

data class Project(
    val id: ProjectId,
    val root: Path,
    val name: String = root.name,
)
