package com.klyx.core.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Appearance {
    @SerialName("light")
    Light,

    @SerialName("dark")
    Dark,

    @SerialName("system")
    System
}
