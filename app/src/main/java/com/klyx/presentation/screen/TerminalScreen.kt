package com.klyx.presentation.screen

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import com.klyx.data.terminal.ExtraTerminalKeys
import com.klyx.data.terminal.KlyxExtraKeysClient
import com.klyx.data.terminal.KlyxTerminalClient
import com.klyx.data.terminal.KlyxTerminalTheme
import com.klyx.data.terminal.TerminalSessionBinder
import com.klyx.data.terminal.TerminalSessionEntry
import com.klyx.data.terminal.TerminalSessionManager
import com.klyx.event.GlobalEventBus
import com.klyx.event.terminal.TerminateAllSessionEvent
import com.klyx.icons.Klyx
import com.klyx.icons.KlyxIcons
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.Navigator
import com.klyx.presentation.navigation.Screen
import com.klyx.presentation.navigation.SettingsScreen
import com.klyx.presentation.viewmodel.TerminalUiState
import com.klyx.presentation.viewmodel.TerminalViewModel
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
import com.klyx.ui.theme.LocalIsDarkMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.uuid.Uuid

private val json = Json { prettyPrint = true; encodeDefaults = true; explicitNulls = false }

@Composable
private fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        callbackFlow {
            trySend(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(true)
                }

                override fun onLost(network: Network) {
                    trySend(false)
                }
            }
            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.collectLatest { isOnline = it }
    }
    return isOnline
}

@OptIn(ExperimentalMaterial3Api::class)
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledIconButton(
                        modifier = Modifier.padding(end = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateTo(SettingsScreen.Terminal) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Terminal Settings")
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
                onTitleChange = {
                    println("title change: $it")
                    sessionTitle = it
                }
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

    val binder = globalOf<TerminalSessionBinder>()
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

    if (uiState.isChecking) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularWavyProgressIndicator()
        }
    } else if (!uiState.isInstalled || uiState.isInstalling || uiState.error != null) {
        TerminalSetup(uiState, viewModel)
    } else {
        TerminalEmulator(
            isServiceBound = isServiceBound,
            navigator = navigator,
            onTitleChange = onTitleChange,
            terminalSettings = terminalSettings
        )
    }
}

@Composable
private fun TerminalSetup(
    uiState: TerminalUiState,
    viewModel: TerminalViewModel
) {
    val isOnline = rememberIsOnline()

    LaunchedEffect(uiState.isInstalled, uiState.isInstalling, uiState.error, isOnline) {
        if (!uiState.isInstalled && !uiState.isInstalling && uiState.error == null && isOnline) {
            viewModel.startInstallation()
        }
    }

    val headerTitle = when {
        uiState.error != null -> "Installation Failed"
        uiState.isInstalling && uiState.currentStep.contains("Download", ignoreCase = true) -> "Downloading Terminal"
        uiState.isInstalling -> "Extracting Environment"
        !isOnline -> "Network Required"
        else -> "Terminal Setup"
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = AbsoluteSmoothCornerShape(32.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.terminal_2_24px),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AnimatedVisibility(visible = !isOnline && !uiState.isInstalling && uiState.error == null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Waiting for network connection...",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.isInstalling) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = uiState.progress,
                            animationSpec = tween<Float>(durationMillis = 300).orSnap(),
                            label = "setup_progress"
                        )

                        Text(
                            text = uiState.currentStep,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )

                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        if (uiState.progressText.isNotEmpty()) {
                            Text(
                                text = uiState.progressText,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.error != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = uiState.error ?: "Unknown error",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.startInstallation() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Retry Installation", fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalEmulator(
    isServiceBound: Boolean,
    navigator: Navigator,
    onTitleChange: (String?) -> Unit,
    terminalSettings: TerminalSettings
) {
    applyTerminalTheme()

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

    val sessionManager = globalOf<TerminalSessionManager>()
    val sessions by sessionManager.sessions.collectAsStateWithLifecycle()
    val currentEntry by sessionManager.currentSession.collectAsStateWithLifecycle()

    val sessionClient = rememberTerminalSessionClient(
        onTitleChanged = { onTitleChange(it.title) },
        cursorStyle = terminalSettings.cursorStyle,
        bellEnabled = terminalSettings.bellEnabled,
        bellVolume = terminalSettings.bellVolume,
        bellSoundType = terminalSettings.bellSoundType
    )

    LaunchedEffect(terminalSettings.cursorStyle) {
        sessions.forEach { entry ->
            entry.session.updateTerminalSessionClient(sessionClient)
        }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(sessions.isEmpty()) {
        if (sessions.isEmpty()) {
            sessionManager.newSession(
                client = sessionClient,
                transcriptRows = terminalSettings.scrollbackLines
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
                currentSessionId = currentEntry?.id,
                onSelect = { id -> sessionManager.switchTo(id) },
                onClose = { id -> scope.launch { sessionManager.terminate(id) } },
                onNewSession = {
                    scope.launch {
                        sessionManager.newSession(
                            client = sessionClient,
                            transcriptRows = terminalSettings.scrollbackLines
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

            if (terminalSettings.extraKeysStyle != ExtraKeyStyle.None) {
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

@SuppressLint("ComposableNaming")
@Composable
private fun applyTerminalTheme() {
    val isDark = LocalIsDarkMode.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val sessionManager = globalOf<TerminalSessionManager>()

    LaunchedEffect(isDark, surfaceColor) {
        KlyxTerminalTheme.apply(isDark, surfaceColor)

        sessionManager.sessions.value.forEach { entry ->
            entry.session.emulator?.colors?.reset()
            entry.session.onColorsChanged()
        }
    }
}

@Composable
private fun TerminalSessionTabs(
    sessions: ImmutableList<TerminalSessionEntry>,
    currentSessionId: Uuid?,
    onSelect: (Uuid) -> Unit,
    onClose: (Uuid) -> Unit,
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    val currentIndex = sessions.indexOfFirst { it.id == currentSessionId }
    LaunchedEffect(currentSessionId, sessions.size) {
        if (currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        shape = AbsoluteSmoothCornerShape(20.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .clip(AbsoluteSmoothCornerShape(16.dp, 60)),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(sessions, key = { _, entry -> entry.id }) { index, entry ->
                    SessionChip(
                        index = index,
                        entry = entry,
                        selected = entry.id == currentSessionId,
                        canClose = sessions.size > 1,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(entry.id)
                        },
                        onClose = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClose(entry.id)
                        },
                        modifier = Modifier.animateItem(
                            fadeInSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow).orSnap(),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                                visibilityThreshold = IntOffset.VisibilityThreshold
                            ).orSnap(),
                            fadeOutSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow).orSnap()
                        )
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            FilledIconButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNewSession()
                },
                modifier = Modifier.size(46.dp),
                shape = AbsoluteSmoothCornerShape(18.dp, 60),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "New terminal session"
                )
            }
        }
    }
}

@Composable
private fun SessionChip(
    index: Int,
    entry: TerminalSessionEntry,
    selected: Boolean,
    canClose: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(220),
        label = "SessionChipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(220),
        label = "SessionChipContent"
    )

    val isRunning by entry.session.isRunning.collectAsStateWithLifecycle()
    val title = entry.session.title?.takeIf { it.isNotBlank() } ?: "Session ${index + 1}"
    val pid = entry.session.pid
    // shellPid: 0 = not yet started, >0 = running pid, -1 = finished (exitStatus valid).
    val pidText = when {
        isRunning && pid > 0 -> "pid $pid"
        pid < 0 -> {
            val status = entry.session.exitStatus
            when {
                status < 0 -> "killed"
                status == 0 -> "finished"
                else -> "exited ($status)"
            }
        }

        else -> "starting..."
    }

    Surface(
        onClick = onClick,
        shape = AbsoluteSmoothCornerShape(18.dp, 60),
        color = container,
        contentColor = content,
        modifier = modifier.height(46.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = if (canClose) 6.dp else 14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.widthIn(max = 150.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = pidText,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = content.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
            if (canClose) {
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close session",
                        tint = content,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
