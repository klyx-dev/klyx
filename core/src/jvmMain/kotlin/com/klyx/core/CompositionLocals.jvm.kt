package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
actual fun ProvideBaseCompositionLocals(content: @Composable () -> Unit) {
    CompositionLocalProvider {
        content()
    }
}
