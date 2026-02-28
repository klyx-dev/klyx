package com.klyx

import androidx.compose.animation.Crossfade
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import arrow.core.raise.context.result
import com.klyx.core.app.Initialization
import com.klyx.core.app.LocalApp
import com.klyx.core.app.trace
import com.klyx.core.cmd.CommandManager
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.io.Paths
import com.klyx.core.io.lastProjectFile
import com.klyx.core.logging.KxLog
import com.klyx.core.logging.MessageType
import com.klyx.core.logging.logerror
import com.klyx.di.LocalKlyxViewModel
import com.klyx.di.LocalStatusBarViewModel
import com.klyx.project.Project
import com.klyx.ui.SplashScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InitScreen() {
    val initState by Initialization.state.collectAsStateWithLifecycle()
    val app = LocalApp.current

    var disclaimerDone by remember { mutableStateOf(false) }
    var readyToEnterMain by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        DisclaimerManager.init(app)
        disclaimerDone = true
    }

    LaunchedEffect(initState.isComplete, disclaimerDone) {
        if (initState.isComplete && disclaimerDone) {
            trace("starting klyx...")
            delay(1000)
            readyToEnterMain = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val klyxViewModel = LocalKlyxViewModel.current
    val openedProject by klyxViewModel.openedProject.collectAsState()

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.subscribeToEvent<KeyEvent> { event ->
            if (event.isCtrlPressed && event.isShiftPressed && event.key == Key.P) {
                CommandManager.showCommandPalette()
            }
        }
    }

    LifecycleStartEffect(Unit) {
        lifecycleScope.launch(Dispatchers.Default) {
            result {
                val project: Project = withContext(Dispatchers.IO) {
                    SystemFileSystem.source(Paths.lastProjectFile).buffered().use { source ->
                        Json.decodeFromString(source.readString())
                    }
                }

                if (project != openedProject) {
                    klyxViewModel.openProject(project)
                }
            }.logerror()
        }

        onStopOrDispose {
            lifecycleScope.launch(Dispatchers.IO) {
                SystemFileSystem.sink(Paths.lastProjectFile).buffered().use { sink ->
                    sink.writeString(Json.encodeToString(openedProject))
                }
            }
        }
    }

    val statusBarViewModel = LocalStatusBarViewModel.current
    val logBuffer = LocalLogBuffer.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default.limitedParallelism(4)) {
            KxLog.logFlow.collect { log ->
                logBuffer.add(log)
                statusBarViewModel.setCurrentLogMessage(log, isProgressive = log.type == MessageType.Progress)
            }
        }
    }

    Crossfade(targetState = readyToEnterMain) { ready ->
        if (ready) MainScreen() else SplashScreen()
    }
}
