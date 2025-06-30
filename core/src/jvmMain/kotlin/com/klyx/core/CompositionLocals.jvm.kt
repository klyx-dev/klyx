package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
actual fun PlatformLocalProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider {
        content()
    }
}
