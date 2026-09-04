package com.klyx.presentation.screen.terminal

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.api.data.preferences.TerminalSettings
import com.klyx.api.data.terminal.TerminalManager
import com.klyx.api.ui.theme.JetBrainsMonoFontFamily
import com.klyx.api.ui.theme.LocalIsDarkMode
import com.klyx.core.globalOf
import com.klyx.data.terminal.ExtraTerminalKeys
import com.klyx.data.terminal.KlyxExtraKeysClient
import com.klyx.data.terminal.KlyxTerminalClient
import com.klyx.data.terminal.KlyxTerminalTheme
import com.klyx.presentation.navigation.Navigator
import com.klyx.terminal.ui.Terminal
import com.klyx.terminal.ui.extrakeys.ExtraKeyStyle
import com.klyx.terminal.ui.extrakeys.ExtraKeys
import com.klyx.terminal.ui.extrakeys.ExtraKeysConstants
import com.klyx.terminal.ui.extrakeys.ExtraKeysInfo
import com.klyx.terminal.ui.extrakeys.rememberExtraKeysState
import com.klyx.terminal.ui.rememberTerminalSessionClient
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { prettyPrint = true; encodeDefaults = true; explicitNulls = false }

@SuppressLint("ComposableNaming")
@Composable
fun applyTerminalTheme() {
    val isDark = LocalIsDarkMode.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    LaunchedEffect(isDark, surfaceColor) {
        KlyxTerminalTheme.apply(isDark, surfaceColor)
    }
}

@Composable
fun TerminalEmulator(
    isServiceBound: Boolean,
    navigator: Navigator,
    onTitleChange: (String?) -> Unit,
    terminalSettings: TerminalSettings
) {
    applyTerminalTheme()

    if (!isServiceBound) {
        TerminalServiceBindingIndicator()
        return
    }

    val sessionManager = globalOf<TerminalManager>().sessionManager
    val sessions by sessionManager.sessions.collectAsStateWithLifecycle()
    val currentEntry by sessionManager.currentSession.collectAsStateWithLifecycle()

    val sessionClient = rememberTerminalSessionClient(
        onTitleChanged = { onTitleChange(it.title) },
        cursorStyle = terminalSettings.cursorStyle,
        bellEnabled = terminalSettings.bellEnabled,
        bellVolume = terminalSettings.bellVolume,
        bellSoundType = terminalSettings.bellSoundType
    )

    LaunchedEffect(terminalSettings.cursorStyle, sessions) {
        sessions.forEach { entry ->
            entry.session.updateTerminalSessionClient(sessionClient)
        }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (sessions.isEmpty()) {
            sessionManager.newSession(
                client = sessionClient,
                transcriptRows = terminalSettings.scrollbackLines,
                showMotd = terminalSettings.showMotd
            )
        }
    }

    LaunchedEffect(currentEntry?.id) {
        onTitleChange(currentEntry?.session?.title)
    }

    val session = currentEntry?.session

    if (session != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            TerminalSessionTabs(
                sessions = sessions,
                activeSessionId = currentEntry?.id,
                onSelectSession = { id -> sessionManager.switchTo(id) },
                onCloseSession = { id -> scope.launch { sessionManager.terminate(id) } },
                onNewSession = {
                    scope.launch {
                        sessionManager.newSession(
                            client = sessionClient,
                            transcriptRows = terminalSettings.scrollbackLines,
                            showMotd = terminalSettings.showMotd
                        )
                    }
                }
            )

            val extraKeysClient = remember(session) { KlyxExtraKeysClient(session) }
            val extraKeysState = rememberExtraKeysState()

            val terminalClient = remember {
                KlyxTerminalClient(
                    extraKeysState = extraKeysState,
                    onFinishRequest = { navigator.navigateBack() }
                )
            }

            key(currentEntry?.id) {
                Terminal(
                    modifier = Modifier.weight(1f),
                    session = session,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = terminalSettings.fontSize.sp,
                    client = terminalClient,
                    cursorBlink = terminalSettings.cursorBlink
                )
            }

            if (terminalSettings.extraKeysStyle != None) {
                ExtraKeys(
                    extraKeysInfo = ExtraKeysInfo(
                        propertiesInfo = json.encodeToString(ExtraTerminalKeys),
                        style = terminalSettings.extraKeysStyle,
                        extraKeyAliasMap = ExtraKeysConstants.CONTROL_CHARS_ALIASES
                    ),
                    state = extraKeysState,
                    client = extraKeysClient,
                    modifier = Modifier.height(75.dp)
                )
            }
        }
    } else {
        TerminalSessionLoading()
    }
}
