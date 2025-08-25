package com.klyx

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.klyx.activities.KlyxActivity
import com.klyx.core.LocalAppSettings
import com.klyx.core.LocalNotifier
import com.klyx.core.LocalSharedPreferences
import com.klyx.core.SharedLocalProvider
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.event.asComposeKeyEvent
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.file.openFile
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.extension.ExtensionManager
import com.klyx.filetree.FileTreeViewModel
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.showWelcome
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : KlyxActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SharedLocalProvider {
                val notifier = LocalNotifier.current
                val prefs = LocalSharedPreferences.current

                val viewModel: EditorViewModel = koinViewModel()
                val fileTreeViewModel = koinViewModel<FileTreeViewModel>()
                val projects by fileTreeViewModel.rootNodes.collectAsState()

                LaunchedEffect(projects) {
                    setTaskDescription(
                        createTaskDescription(
                            if (projects.isEmpty()) {
                                "empty project"
                            } else {
                                projects.joinToString(", ") { it.name }
                            }
                        )
                    )
                }

                var extensionLoadFailure: Throwable? by remember { mutableStateOf(null) }
                LaunchedEffect(Unit) {
                    ExtensionManager.loadExtensions().onFailure {
                        it.printStackTrace()
                        extensionLoadFailure = it
                    }

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

                    //openActivity(RustLspActivity::class)
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

    private fun createTaskDescription(label: String): ActivityManager.TaskDescription {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityManager.TaskDescription.Builder()
                .setLabel(label)
                .build()
        } else {
            @Suppress("DEPRECATION")
            ActivityManager.TaskDescription(label)
        }
    }

//    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
//        lifecycleScope.launch {
//            EventBus.instance.post(event.asComposeKeyEvent())
//        }
//        return super.onKeyShortcut(keyCode, event)
//    }
}
