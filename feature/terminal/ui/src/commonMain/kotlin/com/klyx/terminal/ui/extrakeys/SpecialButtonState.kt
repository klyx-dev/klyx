package com.klyx.terminal.ui.extrakeys

import androidx.compose.runtime.Immutable

@Immutable
data class SpecialButtonState(
    val isActive: Boolean = false,
    val isLocked: Boolean = false,
)
