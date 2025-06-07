package com.klyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.klyx.core.compose.LocalAppSettings
import com.klyx.core.setAppContent
import com.klyx.core.settings.AppTheme
import com.klyx.extension.Extension
import com.klyx.extension.ExtensionToml
import com.klyx.ui.component.editor.EditorScreen
import com.klyx.ui.component.menu.MainMenuBar
import com.klyx.ui.theme.KlyxTheme
import com.klyx.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setAppContent {
            val settings = LocalAppSettings.current
            val isSystemInDarkTheme = isSystemInDarkTheme()

            val darkMode by remember(settings) {
                derivedStateOf {
                    when (settings.theme) {
                        AppTheme.Dark -> true
                        AppTheme.Light -> false
                        AppTheme.System -> isSystemInDarkTheme
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

            LaunchedEffect(Unit) {
                loadTestExtension()
            }

            KlyxTheme(
                dynamicColor = settings.dynamicColors,
                darkTheme = darkMode,
                useThemeExtension = true
            ) {
                val background = MaterialTheme.colorScheme.background

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Column(
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize()
                    ) {
                        MainMenuBar()

                        Surface(color = background) {
                            EditorScreen()
                        }
                    }
                }
            }
        }
    }

    private fun loadTestExtension() {
        try {
            val extensionPath = "extensions/theme_extension"
            val toml = ExtensionToml.from(assets.open("$extensionPath/extension.toml"))

            val themeInput = assets.open("$extensionPath/themes/themes.json")
            
            val extension = Extension(
                wasmInput = null,
                themeInput = themeInput,
                toml = toml,
                path = extensionPath
            )
            
            ThemeManager.loadThemeFamily(extension).onSuccess {
                println(ThemeManager.getAvailableThemes("light"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
