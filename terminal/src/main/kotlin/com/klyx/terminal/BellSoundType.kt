package com.klyx.terminal

import kotlinx.serialization.Serializable

@Serializable
enum class BellSoundType {
    System,
    Gentle,
    VisualOnly
}
