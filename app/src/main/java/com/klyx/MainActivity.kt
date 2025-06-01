package com.klyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.klyx.core.compose.LocalBuildVariant
import com.klyx.core.compose.ProvideCompositionLocals
import com.klyx.core.isDebug
import com.klyx.core.settings.AppTheme
import com.klyx.core.settings.SettingsManager
import com.klyx.core.showShortToast
import com.klyx.editor.compose.EditorProvider
import com.klyx.editor.compose.LocalEditorViewModel
import com.klyx.ui.component.editor.EditorScreen
import com.klyx.ui.theme.KlyxTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val wasm = remember { Wasm(context) }

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

            ProvideCompositionLocals {
                KlyxTheme(
                    dynamicColor = settings.dynamicColors,
                    darkTheme = darkMode
                ) {
                    val bgColor = MaterialTheme.colorScheme.background
                    var color by remember { mutableStateOf(bgColor) }
                    val colorState by animateColorAsState(color)

                    val buildVariant = LocalBuildVariant.current

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colorState
                    ) {
                        Box(
                            modifier = Modifier
                                .systemBarsPadding()
                                .fillMaxSize()
                        ) {
                            EditorProvider {
                                val editorSettings by remember { derivedStateOf { settings.editor } }
                                val viewModel = LocalEditorViewModel.current

                                LaunchedEffect(Unit) {
                                    viewModel.openFile(SettingsManager.settingsFile(context))
                                }

                                EditorScreen(editorSettings)

                                LaunchedEffect(Unit) {
                                    if (buildVariant.isDebug) {
                                        delay(3000)
                                        wasm.test(onColorChanged = { r, g, b ->
                                            color = Color(red = r, green = g, blue = b)
                                            context.showShortToast("Surface color changed for testing, will be reset after 5 seconds")
                                        })
                                        delay(5000)
                                        color = bgColor
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
