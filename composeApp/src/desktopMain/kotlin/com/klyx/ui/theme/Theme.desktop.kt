package com.klyx.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable

@Composable
@ExperimentalMaterial3ExpressiveApi
actual fun KlyxTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    useThemeExtension: Boolean,
    themeName: String?,
    content: @Composable () -> Unit
) {
}
