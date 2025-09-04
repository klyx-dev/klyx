package com.klyx

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.SystemBarStyle
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
import com.blankj.utilcode.util.ClipboardUtils
import com.github.michaelbull.result.onFailure
import com.klyx.activities.KlyxActivity
import com.klyx.core.LocalAppSettings
import com.klyx.core.LocalNotifier
import com.klyx.core.LocalSharedPreferences
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.command
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.event.asComposeKeyEvent
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.file.humanBytes
import com.klyx.core.file.openFile
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.extension.ExtensionManager
import com.klyx.filetree.FileTreeViewModel
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.showWelcome
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.util.concurrent.Executors

class MainActivity : KlyxActivity() {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val notifier = LocalNotifier.current
            val prefs = LocalSharedPreferences.current

            val viewModel: EditorViewModel = koinViewModel()
            val fileTreeViewModel = koinViewModel<FileTreeViewModel>()
            val klyxViewModel = koinViewModel<KlyxViewModel>()
            val projects by fileTreeViewModel.rootNodes.collectAsState()

            LaunchedEffect(projects) {
                setTaskDescription(
                    createTaskDescription(
                        if (projects.isEmpty()) {
                            "empty project"
                        } else {
                            projects.entries.joinToString { it.value.name }
                        }
                    )
                )
            }

            var extensionLoadFailure: String? by remember { mutableStateOf(null) }
            LaunchedEffect(Unit) {
                launch(dispatcher + SupervisorJob()) {
                    notifier.toast("Loading extensions...")
                    ExtensionManager.loadExtensions().onFailure {
                        extensionLoadFailure = it
                    }
                    notifier.toast("Extensions loaded.")
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

            App(themeName = settings.theme) {
                extensionLoadFailure?.let {
                    AlertDialog(
                        onDismissRequest = { extensionLoadFailure = null },
                        text = {
                            Text(
                                text = "Failed to load extensions:\n$it"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { extensionLoadFailure = null }) {
                                Text("OK")
                            }
                        }
                    )
                }

                var showCopiedDialog by remember { mutableStateOf(false) }
                var specs by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    CommandManager.addCommand(command {
                        name("klyx: copy system specs into clipboard")
                        execute {
                            specs = getSystemSpecs()
                            ClipboardUtils.copyText("Klyx", specs)
                            showCopiedDialog = true
                        }
                    })
                }

                if (showCopiedDialog) {
                    AlertDialog(
                        onDismissRequest = { showCopiedDialog = false },
                        title = {
                            Text("Copied into clipboard")
                        },
                        text = {
                            Text(specs)
                        },
                        confirmButton = {
                            TextButton(onClick = { showCopiedDialog = false }) {
                                Text("Ok")
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    )
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

    private fun getSystemSpecs() = buildString {
        appendLine("Klyx: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android Version: ${Build.VERSION.RELEASE}")
        appendLine("Android SDK: ${Build.VERSION.SDK_INT}")
        appendLine("Architecture: ${Build.SUPPORTED_ABIS.first()}")

        val activityManager = getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        appendLine("Memory: ${memoryInfo.totalMem.humanBytes()}")
        append("CPU Count: ${Runtime.getRuntime().availableProcessors()}")
    }

//    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
//        lifecycleScope.launch {
//            EventBus.instance.post(event.asComposeKeyEvent())
//        }
//        return super.onKeyShortcut(keyCode, event)
//    }
}
