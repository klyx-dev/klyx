package com.klyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import com.klyx.core.LocalAppSettings
import com.klyx.core.ProvideBaseCompositionLocals
import com.klyx.core.theme.Appearance
import com.klyx.core.theme.ThemeManager
import com.klyx.core.theme.isDark
import com.klyx.extension.ExtensionFactory
import com.klyx.extension.ExtensionManager
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProvideBaseCompositionLocals {
                val factory: ExtensionFactory = koinInject()

                LaunchedEffect(Unit) {
                    ExtensionManager.loadExtensions(factory)
                }

                val settings = LocalAppSettings.current
                val isSystemInDarkTheme = isSystemInDarkTheme()

                val darkMode by remember(settings) {
                    derivedStateOf {
                        if (settings.dynamicColor) {
                            when (settings.appearance) {
                                Appearance.Dark -> true
                                Appearance.Light -> false
                                Appearance.System -> isSystemInDarkTheme
                            }
                        } else {
                            ThemeManager.getThemeByName(settings.theme)?.isDark() ?: isSystemInDarkTheme
                        }
                    }
                }

                val scrimColor = contentColorFor(MaterialTheme.colorScheme.primary)

                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        darkScrim = scrimColor.toArgb(),
                        lightScrim = scrimColor.toArgb(),
                        detectDarkMode = { darkMode }
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        darkScrim = scrimColor.toArgb(),
                        lightScrim = scrimColor.toArgb(),
                        detectDarkMode = { darkMode }
                    )
                )

                App(
                    darkTheme = darkMode,
                    dynamicColor = settings.dynamicColor,
                    themeName = settings.theme
                )
            }
        }
    }
}
