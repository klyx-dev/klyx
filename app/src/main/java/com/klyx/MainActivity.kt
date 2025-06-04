package com.klyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import com.klyx.core.compose.LocalAppSettings
import com.klyx.core.compose.LocalBuildVariant
import com.klyx.core.event.EventBus
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.setAppContent
import com.klyx.core.settings.AppTheme
import com.klyx.core.settings.SettingsManager
import com.klyx.editor.Editor
import com.klyx.editor.compose.LocalEditorViewModel
import com.klyx.extension.Extension
import com.klyx.extension.ExtensionFactory
import com.klyx.extension.ExtensionToml
import com.klyx.ui.component.editor.EditorScreen
import com.klyx.ui.theme.KlyxTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setAppContent {
            val context = LocalContext.current
            val settings = LocalAppSettings.current

            val extensionFactory = remember { ExtensionFactory.create(context) }
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

            val editorState by LocalEditorViewModel.current.state.collectAsState()
            val editor = Editor.current

            LaunchedEffect(Unit) {
                subscribeToEvent<KeyEvent> { event ->
                    if (event.isCtrlPressed && event.key == Key.S) {

                    }
                }
            }

            KlyxTheme(
                dynamicColor = settings.dynamicColors,
                darkTheme = darkMode
            ) {
                val buildVariant = LocalBuildVariant.current

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize()
                    ) {
                        val viewModel = LocalEditorViewModel.current

                        LaunchedEffect(Unit) {
                            viewModel.openFile(SettingsManager.settingsFile(context))
                        }

                        Box(modifier = Modifier.onPreviewKeyEvent { event ->
                            EventBus.getInstance().postSync(event)
                            false
                        }) {
                            EditorScreen()
                        }

                        LaunchedEffect(Unit) {
                            delay(500)
                            loadTestExtension(extensionFactory)
                        }
                    }
                }
            }
        }
    }

    private fun loadTestExtension(factory: ExtensionFactory) {
        val wasm = assets.open("wasm/my_extension/my_extension.wasm")
        val toml = ExtensionToml.from(assets.open("wasm/my_extension/extension.toml"))

        val extension = Extension(wasm, toml)
        factory.loadExtension(extension, true)
    }
}
