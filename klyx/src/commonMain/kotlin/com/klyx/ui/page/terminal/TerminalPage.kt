package com.klyx.ui.page.terminal

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.LocalNavigator
import com.klyx.core.LocalPlatformContext
import com.klyx.core.app.globalOf
import com.klyx.core.event.EventBus
import com.klyx.core.file.humanBytes
import com.klyx.core.net.isConnected
import com.klyx.core.net.isNotConnected
import com.klyx.core.net.rememberNetworkState
import com.klyx.core.terminal.ExtraKeys
import com.klyx.terminal.FileDownloadStatus
import com.klyx.terminal.SessionBinder
import com.klyx.terminal.SessionManager
import com.klyx.terminal.TerminalManager
import com.klyx.terminal.TerminalUiState
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.event.TerminateAllSessionEvent
import com.klyx.terminal.ui.Terminal
import com.klyx.terminal.ui.extrakeys.ExtraKeyStyle
import com.klyx.terminal.ui.extrakeys.ExtraKeys
import com.klyx.terminal.ui.extrakeys.ExtraKeysConstants
import com.klyx.terminal.ui.extrakeys.ExtraKeysInfo
import com.klyx.terminal.ui.extrakeys.rememberExtraKeysState
import com.klyx.terminal.ui.rememberTerminalSessionClient
import com.klyx.ui.theme.JetBrainsMonoFontFamily
import com.klyx.ui.theme.KlyxMono
import kotlinx.serialization.json.Json

private val json = Json { prettyPrint = true; encodeDefaults = true; explicitNulls = false }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerminalPage(modifier: Modifier = Modifier) {
    InitTerminal { user ->
        var title: String? by remember { mutableStateOf(null) }

        Scaffold(
            modifier = modifier,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            title ?: "Terminal",
                            fontFamily = KlyxMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            }
        ) { innerPadding ->
            val binder = globalOf<SessionBinder>()
            val navigator = LocalNavigator.current
            val context = LocalPlatformContext.current

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                val isBound by binder.isBounded.collectAsState()

                LaunchedEffect(Unit) {
                    EventBus.INSTANCE.subscribe<TerminateAllSessionEvent> {
                        navigator.navigateBack()
                    }
                }

                if (isBound) {
                    val sessionClient = rememberTerminalSessionClient(
                        //onSessionFinished = { navigator.navigateBack() },
                        onTitleChanged = { title = it.title }
                    )

                    var session by remember { mutableStateOf<TerminalSession?>(null) }
                    LaunchedEffect(sessionClient, user) {
                        session = SessionManager.getOrCreateSession(
                            SessionManager.currentSessionId, user, sessionClient
                        )
                        title = session!!.title
                    }

                    if (session != null) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val extraKeysClient = remember(session) { KlyxExtraKeysClient(session!!) }
                            val extraKeysState = rememberExtraKeysState()

                            val terminalClient = remember {
                                KlyxTerminalClient(
                                    extraKeysState = extraKeysState,
                                    onFinishRequest = {
                                        binder.unbind(context)
                                        navigator.navigateBack()
                                    }
                                )
                            }

                            Terminal(
                                modifier = Modifier.weight(1f),
                                session = session!!,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 15.sp,
                                client = terminalClient
                            )

                            ExtraKeys(
                                extraKeysInfo = ExtraKeysInfo(
                                    propertiesInfo = json.encodeToString(ExtraKeys),
                                    style = ExtraKeyStyle.ArrowsOnly,
                                    extraKeyAliasMap = ExtraKeysConstants.CONTROL_CHARS_ALIASES
                                ),
                                state = extraKeysState,
                                client = extraKeysClient,
                                modifier = Modifier.height(75.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Creating session...", fontFamily = KlyxMono)
                            Spacer(Modifier.height(16.dp))
                            CircularWavyProgressIndicator()
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Binding Session", fontFamily = KlyxMono)
                        Spacer(Modifier.height(16.dp))
                        CircularWavyProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun InitTerminal(content: @Composable (user: String) -> Unit) {
    val sessionBinder = globalOf<SessionBinder>()
    val context = LocalPlatformContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> sessionBinder.bind(context)
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val uiState by TerminalManager.uiState.collectAsStateWithLifecycle()

    if (uiState.needsDownload && !TerminalManager.isSandboxExtractionNeeded) {
        TerminalSetup(uiState)
    } else {
        var user by remember { mutableStateOf(TerminalManager.currentUser) }

        if (user != null) {
            content(user!!)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var userName by remember { mutableStateOf("") }
                    val isValid by remember {
                        derivedStateOf {
                            userName.matches("^[a-z][-a-z0-9_]*$".toRegex())
                        }
                    }

                    val errorMessage by remember {
                        derivedStateOf {
                            if (!isValid) "Invalid username" else null
                        }
                    }

                    val createSession = {
                        TerminalManager.currentUser = userName
                        user = userName
                    }

                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it.lowercase().trim() },
                        placeholder = { Text("Enter your username") },
                        supportingText = {
                            Text(
                                text = errorMessage
                                    ?: ("This will create a new user" +
                                            " in the terminal and" +
                                            " will be saved for future sessions."),
                                fontFamily = KlyxMono
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { createSession() }),
                        isError = !isValid,
                        shape = RoundedCornerShape(12.dp),
                    )

                    //Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = createSession,
                        shape = RoundedCornerShape(12.dp),
                        enabled = isValid
                    ) {
                        Text("Create Session", fontFamily = KlyxMono)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TerminalSetup(uiState: TerminalUiState) {
    LaunchedEffect(Unit) {
        if (uiState.needsDownload) {
            TerminalManager.downloadRequiredFiles()
        }
    }

    val networkState by rememberNetworkState()

    val textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = KlyxMono)
    ProvideTextStyle(textStyle) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                if (uiState.error == null && networkState.isConnected) {
                    uiState.files.forEach { file ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(file.displayName)

                                Text(
                                    when (file.status) {
                                        FileDownloadStatus.Pending -> "Pending"
                                        FileDownloadStatus.Downloading -> "${(file.progress * 100).toInt()}%  ${file.downloaded.humanBytes()} / ${file.total.humanBytes()}"
                                        FileDownloadStatus.Extracting -> "Extracting..."
                                        FileDownloadStatus.Done -> "Done"
                                        FileDownloadStatus.Failed -> "Failed"
                                    }
                                )
                            }

                            when (file.status) {
                                FileDownloadStatus.Downloading -> LinearWavyProgressIndicator(
                                    progress = { file.progress },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                FileDownloadStatus.Extracting -> LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth()
                                )

                                FileDownloadStatus.Done -> LinearProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                else -> LinearProgressIndicator(
                                    progress = { 0f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (networkState.isNotConnected) {
                    Text("No internet connection")
                }

                uiState.error?.message?.let { Text(it) }
            }
        }
    }
}
