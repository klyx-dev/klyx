package com.klyx.ui.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.klyx.core.ProvideWindowAdaptiveInfo
import com.klyx.navigation.LocalNavigator
import com.klyx.navigation.rememberNavigator
import com.klyx.ui.theme.FontOption
import com.klyx.ui.theme.KxDarkColors
import com.klyx.ui.theme.LocalKxColors
import com.klyx.ui.theme.LocalKxFont
import com.klyx.ui.theme.ThemeRegistry
import com.klyx.ui.theme.toColorPalette

@Composable
fun ProvideCompositionLocals(
    themeName: String? = null,
    fontName: String? = null,
    content: @Composable () -> Unit
) {
    ProvideWindowAdaptiveInfo {
        val navigator = rememberNavigator()
        val palette by produceState(initialValue = KxDarkColors, themeName) {
            value = themeName?.let { ThemeRegistry.getTheme(themeName)?.toColorPalette() } ?: KxDarkColors
        }
        val fontOption = remember(fontName) { FontOption.fromId(fontName) }

        CompositionLocalProvider(
            LocalNavigator provides navigator,
            LocalKxColors provides palette,
            LocalKxFont provides fontOption,
            content = content
        )
    }
}
