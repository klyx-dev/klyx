package com.klyx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.klyx.core.theme.Contrast
import com.klyx.core.theme.LocalContrast
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.core.theme.ThemeManager
import com.klyx.core.theme.orDefault

@Composable
actual fun KlyxTheme(themeName: String?, content: @Composable (() -> Unit)) {
    val isDarkMode = LocalIsDarkMode.current
    val contrast = LocalContrast.current

    val theme = ThemeManager.getTheme(themeName).orDefault()

    val colorScheme = when (contrast) {
        Contrast.Normal -> {
            if (isDarkMode) theme.darkScheme else theme.lightScheme
        }

        Contrast.Medium -> {
            if (isDarkMode) theme.darkSchemeMediumContrast else theme.lightSchemeMediumContrast
        }

        Contrast.High -> {
            if (isDarkMode) theme.darkSchemeHighContrast else theme.lightSchemeHighContrast
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
