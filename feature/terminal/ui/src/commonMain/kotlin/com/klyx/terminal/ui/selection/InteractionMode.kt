package com.klyx.terminal.ui.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

enum class InteractionMode {
    Touch,
    NonTouch
}

fun InteractionMode.isInTouchMode() = this == InteractionMode.Touch

@Composable
fun rememberInteractionMode() = remember { mutableStateOf(InteractionMode.NonTouch) }
