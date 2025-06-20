package com.klyx.ui.theme

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.klyx.core.theme.ThemeManager

@Composable
@ExperimentalMaterial3ExpressiveApi
actual fun KlyxTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    useThemeExtension: Boolean,
    themeName: String?,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useThemeExtension -> ThemeManager.getColorScheme(darkTheme, themeName)

        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
