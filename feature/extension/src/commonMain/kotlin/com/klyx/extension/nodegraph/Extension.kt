package com.klyx.extension.nodegraph

import androidx.compose.runtime.Immutable
import com.klyx.nodegraph.HeadlessGraph
import kotlinx.io.files.Path

@Immutable
data class Extension(
    val graph: HeadlessGraph,
    val metadata: ExtensionMetadata,
    val filePath: Path,
    val isLocal: Boolean = true
)
