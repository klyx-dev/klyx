package com.klyx

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.klyx.core.LocalAppSettings
import com.klyx.core.LocalBuildVariant
import com.klyx.core.LocalNotifier
import com.klyx.core.LocalSharedPreferences
import com.klyx.core.SharedLocalProvider
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.event.asComposeKeyEvent
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.file.openFile
import com.klyx.core.isDebug
import com.klyx.core.printAllSystemProperties
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.extension.ExtensionManager
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.showWelcome
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SharedLocalProvider {
                val buildVariant = LocalBuildVariant.current
                val notifier = LocalNotifier.current
                val prefs = LocalSharedPreferences.current

                val viewModel: EditorViewModel = koinViewModel()

                var extensionLoadFailure: Throwable? by remember { mutableStateOf(null) }
                LaunchedEffect(Unit) {
                    ExtensionManager.loadExtensions().onFailure { extensionLoadFailure = it }

                    if (prefs.getBoolean("show_welcome", true)) {
                        viewModel.showWelcome()
                        prefs.edit { putBoolean("show_welcome", false) }
                    }

                    subscribeToEvent<CrashEvent> { event ->
                        val isLogFileSaved = event.logFile != null

                        notifier.error(
                            title = "Unexpected error",
                            message = if (isLogFileSaved) "A crash report was saved.\nTap to open." else "Failed to save crash report.",
                            durationMillis = 6000L
                        ) {
                            if (isLogFileSaved) openFile(event.logFile!!)
                        }
                    }

                    startActivity(Intent(this@MainActivity, RustLspActivity::class.java))
                }

                val settings = LocalAppSettings.current
                val darkMode = LocalIsDarkMode.current

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
                ) {
                    extensionLoadFailure?.let {
                        AlertDialog(
                            onDismissRequest = { extensionLoadFailure = null },
                            text = {
                                Text(
                                    text = "Failed to load extensions:\n${it.message}"
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { extensionLoadFailure = null }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    if (buildVariant.isDebug) {
                        printAllSystemProperties()
                    }
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        lifecycleScope.launch {
            EventBus.instance.post(event.asComposeKeyEvent())
        }
        return super.dispatchKeyEvent(event)
    }

//    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
//        lifecycleScope.launch {
//            EventBus.instance.post(event.asComposeKeyEvent())
//        }
//        return super.onKeyShortcut(keyCode, event)
//    }
}
