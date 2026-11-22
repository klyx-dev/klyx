package com.klyx.runtime.workspace

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name

data class Project(
    val id: ProjectId,
    val root: PlatformFile,
    val name: String = root.name,
)
