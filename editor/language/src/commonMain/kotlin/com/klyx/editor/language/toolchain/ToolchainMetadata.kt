package com.klyx.editor.language.toolchain

import com.klyx.editor.language.manifest.ManifestName

/**
 * @property term Returns a term which we should use in UI to refer to toolchains produced by a given [ToolchainLister].
 * @property newToolchainPlaceholder A user-facing placeholder describing the semantic meaning of a path to a new toolchain.
 * @property manifestName The name of the manifest file for this toolchain.
 */
data class ToolchainMetadata(
    val term: String,
    val newToolchainPlaceholder: String,
    val manifestName: ManifestName
)
