package com.klyx.core.ext

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState

@ExperimentalLayoutApi
@Composable
fun rememberImeState(): State<Boolean> {
    val isImeVisible = WindowInsets.isImeVisible
    return rememberUpdatedState(isImeVisible)
}
