package com.klyx.runtime.workspace

import okio.Path

data class Project(
    val id: ProjectId,
    val root: Path,
    val name: String = root.name,
)
