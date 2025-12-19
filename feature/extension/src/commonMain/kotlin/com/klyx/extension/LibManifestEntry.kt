package com.klyx.extension

import io.github.z4kn4fein.semver.Version
import kotlinx.serialization.Serializable

@Serializable
data class LibManifestEntry(
    val kind: ExtensionLibraryKind? = null,
    val version: Version? = null
)
