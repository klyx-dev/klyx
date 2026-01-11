package com.klyx.extension

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SlashCommandManifestEntry(
    val description: String,
    @SerialName("requires_argument")
    val requiresArgument: Boolean
)
