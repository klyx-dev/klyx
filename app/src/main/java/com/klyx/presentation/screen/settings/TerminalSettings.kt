package com.klyx.presentation.screen.settings

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.util.humanBytes
import com.klyx.i18n.strings
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.screen.settings.terminal.BootstrapReinstallDialog
import com.klyx.presentation.screen.settings.terminal.BootstrapSettingsSection
import com.klyx.presentation.screen.settings.terminal.BootstrapUpdateWarningDialog
import com.klyx.presentation.screen.settings.terminal.DisplaySettingsSection
import com.klyx.presentation.screen.settings.terminal.KeyboardSettingsSection
import com.klyx.presentation.screen.settings.terminal.SessionSettingsSection
import com.klyx.presentation.screen.settings.terminal.SoundSettingsSection
import com.klyx.presentation.screen.settings.terminal.TextSettingsSection
import com.klyx.terminal.InstallProgressListener
import com.klyx.terminal.TerminalInstaller
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerminalSettings() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val toastHostState = LocalToastHostState.current
    val s = strings

    val settings = LocalAppSettings.current.terminal
    val terminalInstaller: TerminalInstaller = koinInject()

    var installedVersion by remember { mutableStateOf<String?>(null) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showReinstallDialog by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var updateStep by remember { mutableStateOf("") }
    var updateProgress by remember { mutableFloatStateOf(0f) }
    var updateProgressText by remember { mutableStateOf("") }

    val isInstalledVersionValid = installedVersion != null &&
            TerminalInstaller.isValidBootstrapVersion(installedVersion!!)

    val isUpdateAvailable = latestVersion != null && installedVersion != null &&
            isInstalledVersionValid &&
            latestVersion != installedVersion &&
            TerminalInstaller.isNewer(latestVersion!!, installedVersion!!)

    val isInvalidAndHasLatest = latestVersion != null &&
            installedVersion != null &&
            !isInstalledVersionValid

    LaunchedEffect(Unit) {
        try {
            installedVersion = terminalInstaller.installedVersion()
        } catch (_: Exception) {
            // ignore.
        }
    }

    if (showUpdateDialog) {
        BootstrapUpdateWarningDialog(
            currentVersion = installedVersion ?: "unknown",
            latestVersion = latestVersion ?: "unknown",
            onDismiss = { showUpdateDialog = false },
            onConfirm = {
                showUpdateDialog = false
                isUpdating = true
                updateStep = s.preparing
                updateProgress = 0f
                updateProgressText = ""
                scope.launch {
                    try {
                        terminalInstaller.installLatest(
                            progress = object : InstallProgressListener {
                                override fun step(label: String) {
                                    updateStep = label
                                }

                                override fun progress(done: Long, total: Long) {
                                    val percent = if (total > 0) done.toFloat() / total.toFloat() else 0f
                                    val text = if (updateStep.contains("Downloading", ignoreCase = true)) {
                                        "${done.humanBytes()} / ${total.humanBytes()}"
                                    } else {
                                        s.filesProgress(done.toInt(), total.toInt())
                                    }
                                    updateProgress = percent
                                    updateProgressText = text
                                }

                                override fun warn(message: String) {
                                    Log.w("TerminalSettings", message)
                                }
                            }
                        )
                        installedVersion = terminalInstaller.installedVersion()
                        latestVersion = null
                        launch { toastHostState.showToast(s.bootstrapUpdated) }
                    } catch (e: Exception) {
                        launch { toastHostState.showToast(s.updateFailed(e.message)) }
                    } finally {
                        isUpdating = false
                    }
                }
            }
        )
    }

    if (showReinstallDialog) {
        BootstrapReinstallDialog(
            latestVersion = latestVersion ?: "latest",
            onDismiss = { showReinstallDialog = false },
            onConfirm = {
                showReinstallDialog = false
                isUpdating = true
                updateStep = s.preparing
                updateProgress = 0f
                updateProgressText = ""
                scope.launch {
                    try {
                        terminalInstaller.installLatest(
                            progress = object : InstallProgressListener {
                                override fun step(label: String) {
                                    updateStep = label
                                }

                                override fun progress(done: Long, total: Long) {
                                    val percent = if (total > 0) done.toFloat() / total.toFloat() else 0f
                                    val text = if (updateStep.contains("Downloading", ignoreCase = true)) {
                                        "${done.humanBytes()} / ${total.humanBytes()}"
                                    } else {
                                        s.filesProgress(done.toInt(), total.toInt())
                                    }
                                    updateProgress = percent
                                    updateProgressText = text
                                }

                                override fun warn(message: String) {
                                    Log.w("TerminalSettings", message)
                                }
                            }
                        )
                        installedVersion = terminalInstaller.installedVersion()
                        latestVersion = null
                        launch { toastHostState.showToast(s.bootstrapInstalled) }
                    } catch (e: Exception) {
                        launch { toastHostState.showToast(s.installationFailed(e.message)) }
                    } finally {
                        isUpdating = false
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(strings.terminal) },
                scrollBehavior = scrollBehavior,
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
                            contentDescription = strings.back
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 16.dp
            )
        ) {
            item { SoundSettingsSection(settings = settings, scope = scope) }
            item { TextSettingsSection(settings = settings, scope = scope) }
            item { SessionSettingsSection(settings = settings, scope = scope) }
            item { DisplaySettingsSection(settings = settings, scope = scope) }
            item { KeyboardSettingsSection(settings = settings, scope = scope) }
            item {
                BootstrapSettingsSection(
                    settings = settings,
                    scope = scope,
                    context = context,
                    terminalInstaller = terminalInstaller,
                    installedVersion = installedVersion,
                    latestVersion = latestVersion,
                    isChecking = isChecking,
                    checkError = checkError,
                    isUpdating = isUpdating,
                    updateStep = updateStep,
                    updateProgress = updateProgress,
                    updateProgressText = updateProgressText,
                    isUpdateAvailable = isUpdateAvailable,
                    isInvalidAndHasLatest = isInvalidAndHasLatest,
                    onInstalledVersionChange = { installedVersion = it },
                    onLatestVersionChange = { latestVersion = it },
                    onCheckingChange = { isChecking = it },
                    onCheckErrorChange = { checkError = it },
                    onShowUpdateDialog = { showUpdateDialog = it },
                    onShowReinstallDialog = { showReinstallDialog = it },
                    onUpdatingChange = { isUpdating = it },
                    onUpdateStepChange = { updateStep = it },
                    onUpdateProgressChange = { updateProgress = it },
                    onUpdateProgressTextChange = { updateProgressText = it }
                )
            }
        }
    }
}
