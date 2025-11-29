package com.klyx.core.noderuntime

import kotlinx.io.files.Path

data class NodeBinaryOptions(
    val allowPathLookup: Boolean = true,
    val allowBinaryDownload: Boolean = true,
    val usePaths: Pair<Path, Path>? = null, // node path, npm path
)

