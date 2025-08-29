package com.klyx.core.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Contrast {
    @SerialName("normal")
    Normal,

    @SerialName("medium")
    Medium,

    @SerialName("high")
    High
}
