package com.klyx.core.settings

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TerminalSettings(
    val cursorStyle: CursorStyle = CursorStyle.Block,
    val openAsRoot: Boolean = false
) : KlyxSettings

@Serializable
enum class CursorStyle {
    Block, Underline, Bar
}
