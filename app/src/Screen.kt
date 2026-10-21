package com.klyx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.Theme
import com.klyx.navigation.LocalNavigator
import com.klyx.navigation.Settings
import com.klyx.runtime.settings.SettingsStore
import com.klyx.runtime.settings.resolveActiveThemeName
import com.klyx.runtime.workspace.Workspace
import com.klyx.ui.provider.ProvideCompositionLocals
import com.klyx.ui.screen.HomeScreen
import com.klyx.ui.screen.settings.SettingsScreen
import com.klyx.ui.theme.KlyxTheme
import com.klyx.ui.theme.LocalColorScheme
import com.klyx.ui.theme.LocalCornerRadius
import com.klyx.ui.theme.background
import com.klyx.ui.theme.colors
import org.koin.compose.koinInject

@Composable
fun Screen() {
    val workspace: Workspace = koinInject()
    val settingsStore: SettingsStore = koinInject()

    val settings by settingsStore.settings.collectAsState()

    val roundedUi by workspace.roundedUi.collectAsState()
    val cornerRadius by workspace.cornerRadius.collectAsState()
    val effectiveCornerRadius = if (roundedUi) cornerRadius.dp else 0.dp

    val resolvedThemeName = resolveActiveThemeName(
        themeMode = settings.themeMode,
        darkThemeName = settings.themeName,
        lightThemeName = settings.lightThemeName
    )

    ProvideCompositionLocals(
        themeName = resolvedThemeName,
        fontName = settings.fontName
    ) {
        CompositionLocalProvider(
            LocalCornerRadius provides effectiveCornerRadius,
        ) {
            KlyxTheme(colorScheme = LocalColorScheme.current) {
                val navigator = LocalNavigator.current
                val showSettings = navigator.currentRoute is Settings

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme[colors][background]),
                ) {
                    HomeScreen(modifier = Modifier.fillMaxSize())
                    AnimatedVisibility(
                        visible = showSettings,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        SettingsScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Theme[colors][background]),
                        )
                    }
                }
            }
        }
    }
}
