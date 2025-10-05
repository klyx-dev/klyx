package com.klyx

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import com.klyx.activities.KlyxActivity
import com.klyx.core.LocalAppSettings
import com.klyx.core.LocalNotifier
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.event.asComposeKeyEvent
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.file.humanBytes
import com.klyx.core.file.openFile
import com.klyx.core.file.toKxFile
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.filetree.FileTreeViewModel
import com.klyx.viewmodel.EditorViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : KlyxActivity() {
    private val editorViewModel by viewModel<EditorViewModel>()
    private val fileTreeViewModel by viewModel<FileTreeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val notifier = LocalNotifier.current
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

            LaunchedEffect(Unit) {
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

            AppEntry()
        }
    }

    override fun onResume() {
        super.onResume()
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_EDIT) {
            val uri = intent.data!!
            editorViewModel.openFile(uri.toKxFile())
            setIntent(Intent())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
