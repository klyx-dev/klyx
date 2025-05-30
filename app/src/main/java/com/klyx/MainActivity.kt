package com.klyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.klyx.core.settings.AppTheme
import com.klyx.core.settings.SettingsManager
import com.klyx.editor.compose.EditorProvider
import com.klyx.editor.compose.LocalEditorViewModel
import com.klyx.ui.component.editor.EditorScreen
import com.klyx.ui.theme.KlyxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                SettingsManager.load(context)
            }

            val settings by SettingsManager.settings
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

            KlyxTheme(
                dynamicColor = settings.dynamicColors,
                darkTheme = darkMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.systemBarsPadding()) {
                        EditorProvider {
                            val editorSettings by remember { derivedStateOf { settings.editor } }
                            val viewModel = LocalEditorViewModel.current

                            LaunchedEffect(Unit) {
                                viewModel.openFile(SettingsManager.settingsFile(context))
                            }

                            EditorScreen(editorSettings)
                        }
                    }
                }
            }
        }
    }
}
