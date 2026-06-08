package com.klyx.presentation.screen

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.R
import com.klyx.core.event.subscribe
import com.klyx.core.globalOf
import com.klyx.data.preferences.LocalAppSettings
import com.klyx.data.preferences.TerminalSettings
import com.klyx.data.terminal.ExtraKeys
import com.klyx.data.terminal.FileDownloadState
import com.klyx.data.terminal.FileDownloadStatus
import com.klyx.data.terminal.KlyxExtraKeysClient
import com.klyx.data.terminal.KlyxTerminalClient
import com.klyx.data.terminal.SessionBinder
import com.klyx.data.terminal.SessionManager
import com.klyx.data.terminal.TerminalManager
import com.klyx.event.GlobalEventBus
import com.klyx.event.terminal.TerminateAllSessionEvent
import com.klyx.icons.Klyx
import com.klyx.icons.KlyxIcons
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.Navigator
import com.klyx.presentation.navigation.Screen
import com.klyx.presentation.viewmodel.TerminalUiState
import com.klyx.presentation.viewmodel.TerminalViewModel
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.ui.Terminal
import com.klyx.terminal.ui.extrakeys.ExtraKeyStyle
import com.klyx.terminal.ui.extrakeys.ExtraKeys
import com.klyx.terminal.ui.extrakeys.ExtraKeysConstants
import com.klyx.terminal.ui.extrakeys.ExtraKeysInfo
import com.klyx.terminal.ui.extrakeys.rememberExtraKeysState
import com.klyx.terminal.ui.rememberTerminalSessionClient
import com.klyx.ui.animation.orSnap
import com.klyx.ui.theme.GoogleSansRounded
import com.klyx.ui.theme.JetBrainsMonoFontFamily
import com.klyx.util.humanBytes
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private val json = Json { prettyPrint = true; encodeDefaults = true; explicitNulls = false }

@Composable
private fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }

            override fun onLost(network: Network) {
                isOnline = false
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    return isOnline
}

@Composable
fun TerminalScreen(viewModel: TerminalViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    var sessionTitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        GlobalEventBus.subscribe<TerminateAllSessionEvent> {
            if (navigator.currentScreen is Screen.Terminal) {
                navigator.navigateBack()
            }
        }

        TerminalManager.updateSandboxExtractionState()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = sessionTitle ?: "Terminal",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                },
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Terminal Settings"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            TerminalContent(
                viewModel = viewModel,
                navigator = navigator,
                onTitleChange = { sessionTitle = it }
            )
        }
    }
}

@Composable
private fun TerminalContent(
    viewModel: TerminalViewModel,
    navigator: Navigator,
    onTitleChange: (String?) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val terminalSettings = LocalAppSettings.current.terminal

    val binder = globalOf<SessionBinder>()
    val isServiceBound by binder.isServiceBound.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                binder.bind(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.needsDownload) {
        TerminalSetup(uiState, viewModel)
    } else {
        var user by remember { mutableStateOf(terminalSettings.currentUser) }

        if (user == null && !terminalSettings.openAsRoot) {
            UserSetup { selectedUser ->
                viewModel.setUsername(selectedUser)
                user = selectedUser
            }
        } else {
            val resolvedUser = if (terminalSettings.openAsRoot) "root" else user!!

            TerminalEmulator(
                isServiceBound = isServiceBound,
                user = resolvedUser,
                navigator = navigator,
                onTitleChange = onTitleChange,
                terminalSettings = terminalSettings
            )
        }
    }
}

@Composable
private fun TerminalEmulator(
    isServiceBound: Boolean,
    user: String,
    navigator: Navigator,
    onTitleChange: (String?) -> Unit,
    terminalSettings: TerminalSettings
) {
    if (!isServiceBound) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = AbsoluteSmoothCornerShape(24.dp, 60),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = KlyxIcons.Klyx,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = "Binding Terminal Service",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        return
    }

    val sessionClient = rememberTerminalSessionClient(
        onTitleChanged = { onTitleChange(it.title) },
        cursorStyle = terminalSettings.cursorStyle
    )

    var session by remember { mutableStateOf<TerminalSession?>(null) }

    LaunchedEffect(sessionClient, user) {
        session = SessionManager.currentSessionOrNewSession(
            user = user,
            client = sessionClient
        )
        onTitleChange(session!!.title)
    }

    if (session != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            val extraKeysClient = remember(session) { KlyxExtraKeysClient(session!!) }
            val extraKeysState = rememberExtraKeysState()

            val terminalClient = remember {
                KlyxTerminalClient(
                    extraKeysState = extraKeysState,
                    onFinishRequest = { navigator.navigateBack() }
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
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Creating session...", fontFamily = JetBrainsMonoFontFamily)
            Spacer(Modifier.height(16.dp))
            CircularWavyProgressIndicator()
        }
    }
}

@Composable
private fun UserSetup(onUserCreated: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = AbsoluteSmoothCornerShape(32.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(24.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.terminal_2_24px),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Text(
                    text = "Create Sandbox User",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                var userName by remember { mutableStateOf("") }
                val isValid by remember {
                    derivedStateOf { userName.matches("^[a-z][-a-z0-9_]*$".toRegex()) }
                }

                val errorMessage by remember {
                    derivedStateOf { if (!isValid && userName.isNotEmpty()) "Lowercase letters and numbers only" else null }
                }

                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it.lowercase().trim() },
                    placeholder = { Text("username") },
                    supportingText = {
                        Text(
                            text = errorMessage ?: "Used for terminal login",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (isValid) onUserCreated(userName) }),
                    isError = !isValid && userName.isNotEmpty(),
                    shape = AbsoluteSmoothCornerShape(16.dp, 60),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onUserCreated(userName) },
                    shape = AbsoluteSmoothCornerShape(16.dp, 60),
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Initialize",
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalSetup(
    uiState: TerminalUiState,
    viewModel: TerminalViewModel
) {
    val isOnline = rememberIsOnline()

    LaunchedEffect(uiState.needsDownload, isOnline) {
        if (uiState.needsDownload && isOnline) {
            viewModel.startDownloads()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {

            Icon(
                painter = painterResource(R.drawable.terminal_2_24px),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Initializing Sandbox",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!isOnline && uiState.needsDownload) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "Waiting for network connection...",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }

            if (uiState.error == null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.files.forEach { file ->
                        DownloadItemCard(file = file)
                    }
                }
            }

            uiState.error?.let {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error: $it",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(file: FileDownloadState) {
    val isDone = file.status == FileDownloadStatus.Done

    val animatedProgress by animateFloatAsState(
        targetValue = file.progress,
        animationSpec = tween<Float>(durationMillis = 300).orSnap(),
        label = "download_progress"
    )

    Surface(
        shape = AbsoluteSmoothCornerShape(16.dp, 60),
        color = if (isDone) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = file.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when (file.status) {
                        FileDownloadStatus.Pending -> "Pending"
                        FileDownloadStatus.Downloading -> "${(file.progress * 100).toInt()}%"
                        FileDownloadStatus.Extracting -> "Extracting..."
                        FileDownloadStatus.Done -> "Complete"
                        FileDownloadStatus.Failed -> "Failed"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!isDone) {
                when (file.status) {
                    FileDownloadStatus.Downloading -> {
                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Text(
                            text = "${file.downloaded.humanBytes()} / ${file.total.humanBytes()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    FileDownloadStatus.Extracting -> {
                        LinearWavyProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }

                    FileDownloadStatus.Pending -> {
                        LinearWavyProgressIndicator(
                            progress = { 0f },
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }

                    FileDownloadStatus.Failed -> {
                        LinearProgressIndicator(
                            progress = { 1f },
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
