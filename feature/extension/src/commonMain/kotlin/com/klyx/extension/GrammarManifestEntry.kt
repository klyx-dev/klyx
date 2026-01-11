package com.klyx.extension

import com.klyx.core.util.path.OkioPathSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import okio.Path

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GrammarManifestEntry(
    val repository: String,
    @JsonNames("commit")
    val rev: String,
    @Serializable(OkioPathSerializer::class)
    val path: Path? = null
)
