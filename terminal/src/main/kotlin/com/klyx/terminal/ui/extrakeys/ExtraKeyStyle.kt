package com.klyx.terminal.ui.extrakeys

import kotlinx.serialization.Serializable

@Serializable
enum class ExtraKeyStyle {
    ArrowsOnly,
    ArrowsAll,
    All,
    None,
    Default
}
