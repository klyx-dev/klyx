package com.klyx.extension

import com.klyx.core.io.Path
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GrammarManifestEntry(
    val repository: String,
    @JsonNames("commit")
    val rev: String,
    val path: Path? = null
)
