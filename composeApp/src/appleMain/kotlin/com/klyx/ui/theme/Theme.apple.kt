package com.klyx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
actual fun KlyxTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    useThemeExtension: Boolean,
    themeName: String?,
    content: @Composable (() -> Unit)
) {
    MaterialTheme(
        typography = AppTypography,
        content = content
    )
}
