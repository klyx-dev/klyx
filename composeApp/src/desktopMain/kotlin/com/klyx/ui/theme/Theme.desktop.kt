package com.klyx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.klyx.core.theme.ThemeManager

@Composable
actual fun KlyxTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    useThemeExtension: Boolean,
    themeName: String?,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useThemeExtension -> ThemeManager.getColorScheme(darkTheme, themeName)
        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
