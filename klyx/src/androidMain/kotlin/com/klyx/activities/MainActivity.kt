package com.klyx.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import com.klyx.InitScreen
import com.klyx.core.Notifier
import com.klyx.core.app.GlobalApp
import com.klyx.core.app.UnsafeGlobalAccess
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.event.Subscriber
import com.klyx.core.event.asComposeKeyEvent
import com.klyx.core.event.registerSubscriber
import com.klyx.core.file.openFile
import com.klyx.core.file.toKxFile
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.filetree.FileTreeViewModel
import com.klyx.project.toWorktree
import com.klyx.terminal.SessionBinder
import com.klyx.terminal.event.TerminateAllSessionEvent
import com.klyx.terminal.service.SessionService
import com.klyx.ui.event.TerminalNotificationTapEvent
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : KlyxActivity(), Subscriber<CrashEvent> {

    private val notifier by inject<Notifier>()

    private val editorViewModel by viewModel<EditorViewModel>()
    private val fileTreeViewModel by viewModel<FileTreeViewModel>()
    private val klyxVm by viewModel<KlyxViewModel>()

    @OptIn(UnsafeGlobalAccess::class)
    private val app by lazy { GlobalApp }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        registerSubscriber(this)

        EventBus.INSTANCE.subscribe<TerminateAllSessionEvent> {
            app.global<SessionBinder>().unbind(this)
        }
    }

    @Composable
    override fun Content() {
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

        InitScreen()
    }

    override fun onResume() {
        super.onResume()
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        app.global<SessionBinder>().unbind(this)
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.data

        if (uri != null && uri.scheme == "klyx") {
            when (uri.host) {
                "open" -> {
                    val path = uri.getQueryParameter("project")?.let { Uri.decode(it) }
                        ?: uri.getQueryParameter("file")?.let { Uri.decode(it) }

                    if (!path.isNullOrEmpty()) {
                        val file = path.toKxFile()
                        if (file.exists) {
                            if (file.isDirectory) {
                                klyxVm.openProject(file.toWorktree())
                                notifier.notify(
                                    title = "Project opened",
                                    message = file.name
                                )
                            } else if (file.isFile) {
                                editorViewModel.openFile(file)
                            }
                        } else {
                            notifier.error(
                                title = "Invalid path",
                                message = "${file.absolutePath} does not exist"
                            )
                        }
                    }
                }
            }
            setIntent(Intent())
            return
        }

        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_EDIT) {
            if (uri != null) {
                editorViewModel.openFile(uri.toKxFile())
            }
            setIntent(Intent())
        } else if (intent.action == SessionService.ACTION_NOTIFICATION_TAP) {
            EventBus.INSTANCE.tryPost(TerminalNotificationTapEvent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        lifecycleScope.launch {
            EventBus.INSTANCE.post(event.asComposeKeyEvent())
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

    override suspend fun onEvent(event: CrashEvent) {
        val isLogFileSaved = event.logFile != null

        notifier.error(
            title = "Unexpected error",
            message = if (isLogFileSaved) "A crash report was saved.\nTap to open." else "Failed to save crash report.",
            durationMillis = 6000L
        ) {
            if (isLogFileSaved) openFile(event.logFile!!)
        }
    }

//    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
//        lifecycleScope.launch {
//            EventBus.INSTANCE.post(event.asComposeKeyEvent())
//        }
//        return super.onKeyShortcut(keyCode, event)
//    }
}
