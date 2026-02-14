package com.klyx.core

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
actual fun PlatformLocalProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider {
        content()
    }
}

@Composable
actual fun dynamicDarkColorScheme(): ColorScheme {
    return darkColorScheme()
}

@Composable
actual fun dynamicLightColorScheme(): ColorScheme {
    return lightColorScheme()
}
