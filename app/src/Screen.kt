package com.klyx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.composeunstyled.theme.Theme
import com.klyx.navigation.LocalNavigator
import com.klyx.navigation.Settings
import com.klyx.ui.KlyxTheme
import com.klyx.ui.LocalColorScheme
import com.klyx.ui.background
import com.klyx.ui.colors
import com.klyx.ui.provider.ProvideCompositionLocals
import com.klyx.ui.screen.HomeScreen
import com.klyx.ui.screen.SettingsScreen

@Composable
fun Screen() {
    ProvideCompositionLocals {
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
