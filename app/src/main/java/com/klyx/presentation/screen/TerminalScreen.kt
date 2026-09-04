package com.klyx.presentation.screen

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.data.terminal.TerminalManager
import com.klyx.api.event.terminal.TerminateAllSessionEvent
import com.klyx.api.ui.theme.JetBrainsMonoFontFamily
import com.klyx.core.event.subscribe
import com.klyx.core.globalOf
import com.klyx.event.GlobalEventBus
import com.klyx.i18n.strings
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.Navigator
import com.klyx.presentation.navigation.Screen
import com.klyx.presentation.navigation.SettingsScreen
import com.klyx.presentation.screen.terminal.TerminalEmulator
import com.klyx.presentation.screen.terminal.TerminalSetup
import com.klyx.presentation.viewmodel.TerminalViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(viewModel: TerminalViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    var sessionTitle by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val subscription = GlobalEventBus.subscribe<TerminateAllSessionEvent>(
            dispatcher = Dispatchers.Main.immediate
        ) {
            if (navigator.currentScreen is Screen.Terminal) {
                navigator.navigateBack()
            }
        }
        onDispose { subscription.cancel() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = sessionTitle ?: strings.terminal,
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
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
                        Icon(Icons.Outlined.Settings, contentDescription = strings.terminalSettings)
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
                    sessionTitle = it
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TerminalContent(
    viewModel: TerminalViewModel,
    navigator: Navigator,
    onTitleChange: (String?) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val terminalSettings = LocalAppSettings.current.terminal

    val binder = globalOf<TerminalManager>().sessionBinder
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
        Box(Modifier.fillMaxSize(), contentAlignment = Center) {
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
