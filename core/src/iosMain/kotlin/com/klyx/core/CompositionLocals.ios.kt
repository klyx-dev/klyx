package com.klyx.core

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
actual fun PlatformLocalProvider(content: @Composable (() -> Unit)) {
    CompositionLocalProvider(content = content)
}

@Composable
actual fun dynamicDarkColorScheme(): ColorScheme {
    return darkColorScheme()
}
