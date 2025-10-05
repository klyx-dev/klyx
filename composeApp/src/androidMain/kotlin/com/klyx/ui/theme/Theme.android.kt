package com.klyx.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.klyx.core.LocalAppSettings
import com.klyx.core.theme.Contrast
import com.klyx.core.theme.LocalContrast
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.core.theme.ThemeManager
import com.klyx.core.theme.orDefault

@Composable
fun KlyxTheme(themeName: String?, content: @Composable (() -> Unit)) {
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val isDarkMode = LocalIsDarkMode.current
    val contrast = LocalContrast.current

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && settings.dynamicColor) {
        if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val theme = ThemeManager.getTheme(themeName).orDefault()

        when (contrast) {
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
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
