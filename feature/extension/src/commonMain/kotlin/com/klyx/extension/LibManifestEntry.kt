package com.klyx.extension

import io.github.z4kn4fein.semver.Version
import kotlinx.serialization.Serializable

@Serializable
data class LibManifestEntry(
    var kind: ExtensionLibraryKind? = null,
    var version: Version? = null
)
