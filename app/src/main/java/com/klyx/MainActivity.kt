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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.klyx.core.compose.LocalAppSettings
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.file.KWatchEvent
import com.klyx.core.file.asWatchChannel
import com.klyx.core.file.id
import com.klyx.core.file.isTextEqualTo
import com.klyx.core.setAppContent
import com.klyx.core.settings.AppTheme
import com.klyx.core.settings.SettingsManager
import com.klyx.editor.compose.LocalEditorViewModel
import com.klyx.editor.compose.rememberCurrentEditor
import com.klyx.extension.Extension
import com.klyx.extension.ExtensionFactory
import com.klyx.extension.ExtensionToml
import com.klyx.ui.component.editor.EditorScreen
import com.klyx.ui.component.editor.MainMenuBar
import com.klyx.ui.theme.KlyxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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

            val editor by rememberCurrentEditor()
            val editorState by LocalEditorViewModel.current.state.collectAsState()
            val settingsFile = remember { SettingsManager.settingsFile(context) }

            LaunchedEffect(Unit) {
                subscribeToEvent<KeyEvent> { event ->
                    when {
                        event.type == KeyEventType.KeyDown -> {
                            if (event.isCtrlPressed && event.key == Key.S) {
                                println(event.type)
                                if (editorState.activeFileId == settingsFile.id) {
                                    println("Saving file")

                                    lifecycleScope.launch(Dispatchers.IO) {
                                        settingsFile.writeText(editor?.text.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DisposableEffect(Unit) {
                var settingText = settingsFile.readText()
                val watchChannel = settingsFile.asWatchChannel()

                lifecycleScope.launch(Dispatchers.IO) {
                    watchChannel.consumeEach { event ->
                        if (event.kind == KWatchEvent.Kind.Modified) {
                            if (event.file.isTextEqualTo(settingText)) return@consumeEach

                            settingText = event.file.readText()
                            SettingsManager.load(context)
                        }
                    }
                }

                onDispose { watchChannel.close() }
            }

            KlyxTheme(
                dynamicColor = settings.dynamicColors,
                darkTheme = darkMode
            ) {
                val background = MaterialTheme.colorScheme.background

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize()
                    ) {
                        MainMenuBar()

                        Surface(color = background) {
                            val viewModel = LocalEditorViewModel.current
                            LaunchedEffect(Unit) {
                                viewModel.openFile(settingsFile)

                                val tempFile = File.createTempFile("temp", ".txt").apply { deleteOnExit() }
                                tempFile.writeText("Hello, World!")
                                viewModel.openFile(tempFile)
                            }

                            EditorScreen()

                            LaunchedEffect(Unit) {
                                delay(500)
                                loadTestExtension(extensionFactory)
                            }
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
