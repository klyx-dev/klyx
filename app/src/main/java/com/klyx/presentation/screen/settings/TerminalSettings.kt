package com.klyx.presentation.screen.settings

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TextFormat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klyx.data.fs.Paths
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.data.preferences.updateTerminalSettings
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SliderSettingsItem
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import com.klyx.terminal.BellSoundType
import com.klyx.terminal.BootstrapUpdateChecker
import com.klyx.terminal.InstallProgressListener
import com.klyx.terminal.SafExposureState
import com.klyx.terminal.TerminalDocumentsProvider
import com.klyx.terminal.TerminalInstaller
import com.klyx.terminal.emulator.CursorStyle
import com.klyx.terminal.rootFs
import com.klyx.terminal.ui.extrakeys.ExtraKeyStyle
import com.klyx.api.ui.theme.GoogleSansRounded
import com.klyx.ui.widgets.LocalToastHostState
import com.klyx.util.humanBytes
import com.klyx.util.sliderSteps
import kotlinx.collections.immutable.toImmutableList
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
            //latestVersion = BootstrapUpdateChecker.latestVersion()
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
                updateStep = "Preparing..."
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
                                        "$done / $total files"
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
                        launch { toastHostState.showToast("Bootstrap updated successfully") }
                    } catch (e: Exception) {
                        launch { toastHostState.showToast("Update failed: ${e.message}") }
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
                updateStep = "Preparing..."
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
                                        "$done / $total files"
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
                        launch { toastHostState.showToast("Bootstrap installed successfully") }
                    } catch (e: Exception) {
                        launch { toastHostState.showToast("Installation failed: ${e.message}") }
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
                title = { Text("Terminal") },
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
                            contentDescription = "Back"
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
            item {
                SettingsSubsection(title = "Sound") {
                    SwitchSettingItem(
                        title = "Bell Sound",
                        subtitle = "Play a sound when the terminal bell (BEL) is received.",
                        checked = settings.bellEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                updateTerminalSettings { copy(bellEnabled = enabled) }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (settings.bellEnabled) Icons.Rounded.Notifications
                                else Icons.Rounded.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = settings.bellEnabled,
                        enter = expandVertically(
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                        ) + fadeIn(animationSpec = spring(stiffness = 400f)),
                        exit = shrinkVertically(animationSpec = spring(stiffness = 500f)) + fadeOut(
                            animationSpec = spring(stiffness = 500f)
                        )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            SliderSettingsItem(
                                label = "Bell Volume",
                                value = settings.bellVolume,
                                valueRange = 0f..1f,
                                steps = (0f..1f).sliderSteps(increment = 0.1f),
                                onValueChange = { volume ->
                                    scope.launch {
                                        updateTerminalSettings { copy(bellVolume = volume) }
                                    }
                                },
                                valueText = { "${(it * 100).toInt()}%" }
                            )

                            SelectorItem(
                                label = "Bell Sound Type",
                                description = "Choose the tone played for the terminal bell.",
                                options = BellSoundType.entries.toImmutableList(),
                                selected = settings.bellSoundType,
                                optionLabel = { type ->
                                    when (type) {
                                        BellSoundType.Gentle -> "Gentle"
                                        BellSoundType.System -> "System"
                                        BellSoundType.VisualOnly -> "Visual Only"
                                    }
                                },
                                optionDescription = { type ->
                                    when (type) {
                                        BellSoundType.Gentle -> "A soft acknowledgment tone"
                                        BellSoundType.System -> "The traditional alert beep"
                                        BellSoundType.VisualOnly -> "No sound, visual feedback only"
                                    }
                                },
                                onSelectionChanged = { selectedType ->
                                    scope.launch {
                                        updateTerminalSettings { copy(bellSoundType = selectedType) }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSubsection(title = "Text") {
                    SliderSettingsItem(
                        label = "Font Size",
                        value = settings.fontSize,
                        valueRange = 8f..30f,
                        steps = (8f..30f).sliderSteps(1f),
                        onValueChange = { size ->
                            scope.launch {
                                updateTerminalSettings { copy(fontSize = size) }
                            }
                        },
                        valueText = { "${it.toInt()}sp" }
                    )

                    SwitchSettingItem(
                        title = "Cursor Blinking",
                        subtitle = "Make the terminal cursor blink.",
                        checked = settings.cursorBlink,
                        onCheckedChange = { blink ->
                            scope.launch {
                                updateTerminalSettings { copy(cursorBlink = blink) }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FlashOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )

                    SelectorItem(
                        label = "Cursor Style",
                        description = "Choose the shape of the terminal cursor.",
                        options = CursorStyle.availableStyles().toImmutableList(),
                        selected = settings.cursorStyle,
                        optionLabel = { style ->
                            when (style) {
                                CursorStyle.Block -> "Block"
                                CursorStyle.Underline -> "Underline"
                                CursorStyle.Bar -> "Bar"
                                else -> "Unknown"
                            }
                        },
                        optionDescription = { style ->
                            when (style) {
                                CursorStyle.Block -> "A solid rectangle after the character"
                                CursorStyle.Underline -> "A horizontal line below the character"
                                CursorStyle.Bar -> "A thin vertical line after the character"
                                else -> null
                            }
                        },
                        onSelectionChanged = { selectedStyle ->
                            scope.launch {
                                updateTerminalSettings { copy(cursorStyle = selectedStyle) }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.TextFormat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                }
            }

            item {
                SettingsSubsection(title = "Session") {
                    SwitchSettingItem(
                        title = "Show MOTD",
                        subtitle = "Display the message of the day when a new terminal session is created.",
                        checked = settings.showMotd,
                        onCheckedChange = { showMotd ->
                            scope.launch {
                                updateTerminalSettings { copy(showMotd = showMotd) }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                }
            }

            item {
                SettingsSubsection(title = "Display") {
                    SliderSettingsItem(
                        label = "Scrollback Lines",
                        value = settings.scrollbackLines.toFloat(),
                        valueRange = 100f..50000f,
                        steps = 0,
                        onValueChange = { lines ->
                            scope.launch {
                                updateTerminalSettings { copy(scrollbackLines = lines.toInt()) }
                            }
                        },
                        valueText = { "${it.toInt()}" }
                    )
                }
            }

            item {
                SettingsSubsection(title = "Keyboard") {
                    SelectorItem(
                        label = "Extra Keys Style",
                        description = "Choose the layout of the extra keys toolbar.",
                        options = ExtraKeyStyle.entries.toImmutableList(),
                        selected = settings.extraKeysStyle,
                        optionLabel = { style ->
                            when (style) {
                                ExtraKeyStyle.ArrowsOnly -> "Arrows Only"
                                ExtraKeyStyle.ArrowsAll -> "Arrows All"
                                ExtraKeyStyle.All -> "All"
                                ExtraKeyStyle.None -> "None"
                                ExtraKeyStyle.Default -> "Default"
                            }
                        },
                        optionDescription = { style ->
                            when (style) {
                                ExtraKeyStyle.ArrowsOnly -> "Only arrow keys"
                                ExtraKeyStyle.ArrowsAll -> "Extended arrow keys"
                                ExtraKeyStyle.All -> "Full ISO keyboard layout"
                                ExtraKeyStyle.None -> "No extra keys"
                                ExtraKeyStyle.Default -> "Default layout"
                            }
                        },
                        onSelectionChanged = { selectedStyle ->
                            scope.launch {
                                updateTerminalSettings { copy(extraKeysStyle = selectedStyle) }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Keyboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                }
            }

            item {
                SettingsSubsection(title = "Bootstrap") {
                    VersionInfoCard(
                        label = "Installed Version",
                        value = installedVersion ?: "Not installed",
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = if (installedVersion != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    if (latestVersion != null) {
                        VersionInfoCard(
                            label = "Latest Version",
                            value = latestVersion!!,
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        )
                    }

                    if (installedVersion == null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Opening the terminal screen will automatically download and install the latest bootstrap.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (isUpdating) {
                        BootstrapUpdateProgressDialog(
                            step = updateStep,
                            progress = updateProgress,
                            progressText = updateProgressText
                        )
                    }

                    isChecking.BootstrappingRow {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LoadingIndicator(modifier = Modifier.size(24.dp))

                            Text(
                                text = "Checking for updates...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (checkError != null) {
                        ErrorBootstrapCard(
                            error = checkError!!,
                            onRetry = {
                                isChecking = true
                                checkError = null
                                scope.launch {
                                    try {
                                        installedVersion = terminalInstaller.installedVersion()
                                        latestVersion = BootstrapUpdateChecker.latestVersion()
                                    } catch (e: Exception) {
                                        checkError = e.message ?: "Unknown error"
                                    } finally {
                                        isChecking = false
                                    }
                                }
                            }
                        )
                    }

                    if (isUpdateAvailable) {
                        UpdateAvailableBootstrapCard(
                            from = installedVersion ?: "?",
                            to = latestVersion ?: "?",
                            onClick = { showUpdateDialog = true }
                        )
                    }

                    if (isInvalidAndHasLatest) {
                        InvalidBootstrapVersionCard(
                            version = installedVersion ?: "?",
                            onClick = { showReinstallDialog = true }
                        )
                    }

                    if (latestVersion != null && installedVersion != null && latestVersion == installedVersion) {
                        UpToDateBootstrapCard()
                    }

                    if (!isChecking && !isUpdating && checkError == null && !isUpdateAvailable && !isInvalidAndHasLatest && !(latestVersion != null && installedVersion != null && latestVersion == installedVersion)) {
                        CheckForUpdatesBootstrapCard(
                            onCheck = {
                                isChecking = true
                                checkError = null
                                scope.launch {
                                    try {
                                        installedVersion = terminalInstaller.installedVersion()
                                        latestVersion = BootstrapUpdateChecker.latestVersion()
                                    } catch (e: Exception) {
                                        checkError = e.message ?: "Unknown error"
                                    } finally {
                                        isChecking = false
                                    }
                                }
                            }
                        )
                    }

                    var safExposed by remember { mutableStateOf(settings.exposeTerminalHomeViaSaf) }
                    LaunchedEffect(settings.exposeTerminalHomeViaSaf) {
                        safExposed = settings.exposeTerminalHomeViaSaf
                    }

                    SwitchSettingItem(
                        title = "Expose Terminal Home via SAF",
                        subtitle = "Allow file managers to access the terminal home directory through the Storage Access Framework.",
                        checked = safExposed,
                        onCheckedChange = { enabled ->
                            safExposed = enabled
                            SafExposureState.enabled = enabled
                            TerminalDocumentsProvider.notifyRootsChanged(context)
                            scope.launch {
                                updateTerminalSettings { copy(exposeTerminalHomeViaSaf = enabled) }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Boolean.BootstrappingRow(content: @Composable () -> Unit) {
    if (this) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        ) {
            Box(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun ErrorBootstrapCard(error: String, onRetry: () -> Unit) {
    Surface(
        onClick = onRetry,
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Check failed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Retry",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun UpdateAvailableBootstrapCard(from: String, to: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Update Available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$from -> $to",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Update",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )
        }
    }
}

@Composable
private fun InvalidBootstrapVersionCard(version: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Corrupted Version File",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Current version \"$version\" is invalid. Reinstall terminal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Reinstall",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )
        }
    }
}

@Composable
private fun UpToDateBootstrapCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Bootstrap is up to date",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CheckForUpdatesBootstrapCard(onCheck: () -> Unit) {
    Surface(
        onClick = onCheck,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Check for Updates",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Fetch the latest bootstrap version from GitHub",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VersionInfoCard(
    label: String,
    value: String,
    icon: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                icon()
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BootstrapReinstallDialog(
    latestVersion: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Reinstall Terminal?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = buildAnnotatedString {
                        append("The current version file is corrupted or invalid. The terminal will be reinstalled fresh with version $latestVersion.\n\n")
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            append($$"Do NOT store your projects or important files inside $PREFIX. They will be permanently deleted.\n\n")
                        }
                        append($$"Store your work in $HOME or other safe directories instead.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ButtonDefaults.MediumContainerHeight),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ButtonDefaults.MediumContainerHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            text = "Reinstall",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BootstrapUpdateWarningDialog(
    currentVersion: String,
    latestVersion: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Update Bootstrap?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$currentVersion -> $latestVersion",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = buildAnnotatedString {
                        append($$"This will download and extract the latest bootstrap, replacing all files in $ROOTFS ($${Paths.rootFs.absolutePath}/).\n\n")
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            append($$"Do NOT store your projects or important files inside $PREFIX. They will be permanently deleted.\n\n")
                        }
                        append($$"Store your work in $HOME or other safe directories instead.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ButtonDefaults.MediumContainerHeight),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ButtonDefaults.MediumContainerHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Update",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BootstrapUpdateProgressDialog(
    step: String,
    progress: Float,
    progressText: String
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
//                    CircularWavyProgressIndicator(
//                        modifier = Modifier.size(36.dp),
//                        color = MaterialTheme.colorScheme.onPrimaryContainer,
//                        trackColor = MaterialTheme.colorScheme.primaryContainer
//                    )
                    LoadingIndicator()
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Updating Bootstrap...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee(),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                if (progressText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
