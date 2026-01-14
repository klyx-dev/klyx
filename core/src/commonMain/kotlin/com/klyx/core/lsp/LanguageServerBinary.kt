package com.klyx.core.lsp

import com.klyx.core.serializers.path.PathSerializer
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable

/**
 * Represents a launchable language server. This can either be a standalone binary or the path
 * to a runtime with arguments to instruct it to launch the actual language server file.
 */
@Serializable
data class LanguageServerBinary(
    @Serializable(with = PathSerializer::class)
    val path: Path,
    var arguments: List<String>,
    var env: HashMap<String, String>?
)
