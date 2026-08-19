package com.klyx.presentation.screen.settings

import com.klyx.i18n.strings
import com.klyx.i18n.getLocaleStrings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klyx.BuildConfig
import com.klyx.api.data.preferences.AppSettings
import com.klyx.api.platform.currentArchitecture
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.ui.theme.GoogleSansRounded
import com.klyx.app.icons.BugReport
import com.klyx.app.icons.DeleteSweep
import com.klyx.app.icons.FileDownload
import com.klyx.app.icons.FileUpload
import com.klyx.app.icons.Unarchive
import com.klyx.data.preferences.SettingsRepository
import com.klyx.data.preferences.settingsJson
import com.klyx.presentation.components.dialogs.TerminalWipeConfirmationDialog
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.SettingsScreen
import com.klyx.presentation.screen.settings.components.SettingsItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.terminal.InstallProgressListener
import com.klyx.terminal.TerminalInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperOptionsScreen() {
    val navigator = LocalNavigator.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val toastHostState = LocalToastHostState.current
    val terminalInstaller: TerminalInstaller = koinInject()
    val settingsRepository: SettingsRepository = koinInject()
    val context = LocalContext.current

    var showWipeDialog by remember { mutableStateOf(false) }
    var showAssetDialog by remember { mutableStateOf(false) }
    var isInstallingAsset by remember { mutableStateOf(false) }
    var assetInstallLabel by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val s = getLocaleStrings()
            try {
                val settings = settingsRepository.settings.first()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        @OptIn(ExperimentalSerializationApi::class)
                        settingsJson.encodeToStream(settings, output)
                    }
                }
                toastHostState.showToast(s.settingsExported)
            } catch (e: Exception) {
                toastHostState.showToast(s.exportFailed(e.message))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val s = getLocaleStrings()
            try {
                val imported = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: return@withContext null
                    stream.use { input ->
                        @OptIn(ExperimentalSerializationApi::class)
                        settingsJson.decodeFromStream<AppSettings>(input)
                    }
                }
                if (imported != null) {
                    settingsRepository.updateSettings { imported }
                    toastHostState.showToast(s.settingsImported)
                } else {
                    toastHostState.showToast(s.importFailedCouldNotRead)
                }
            } catch (e: SerializationException) {
                toastHostState.showToast(s.importFailedInvalidSettings)
            } catch (e: Exception) {
                toastHostState.showToast(s.exportFailed(e.message))
            }
        }
    }

    if (showWipeDialog) {
        TerminalWipeConfirmationDialog(
            onDismiss = { showWipeDialog = false },
            onConfirm = {
                showWipeDialog = false
                scope.launch {
                    val s = getLocaleStrings()
                    terminalInstaller.uninstall()
                    toastHostState.showToast(s.terminalEnvWiped)
                }
            }
        )
    }

    if (showAssetDialog) {
        val assetName = "bootstrap-${currentArchitecture()}.zip"
        AssetBootstrapConfirmationDialog(
            assetName = assetName,
            onDismiss = { showAssetDialog = false },
            onConfirm = {
                showAssetDialog = false
                isInstallingAsset = true
                assetInstallLabel = getLocaleStrings().starting
                scope.launch {
                    val s = getLocaleStrings()
                    try {
                        terminalInstaller.installFromAsset(
                            assetName = assetName,
                            progress = object : InstallProgressListener {
                                override fun step(label: String) {
                                    assetInstallLabel = label
                                }

                                override fun progress(done: Long, total: Long) {}
                                override fun warn(message: String) {}
                            }
                        )
                        launch { toastHostState.showToast(s.bootstrapInstalledFromAssets) }
                    } catch (e: Exception) {
                        launch { toastHostState.showToast(s.installationFailed(e.message)) }
                    } finally {
                        isInstallingAsset = false
                    }
                }
            }
        )
    }

    if (isInstallingAsset) {
        AssetInstallProgressDialog(label = assetInstallLabel)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(strings.developerOptions) },
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
            developerOptionsList(
                onWipeClick = { showWipeDialog = true },
                onLogsClick = { navigator.navigateTo(SettingsScreen.Logs) },
                onExportClick = { exportLauncher.launch("klyx-settings.json") },
                onImportClick = { importLauncher.launch(arrayOf("application/json")) },
                onAssetInstallClick = { showAssetDialog = true }
            )
        }
    }
}

/** All items shown in the developer options [LazyColumn]. */
private fun LazyListScope.developerOptionsList(
    onWipeClick: () -> Unit,
    onLogsClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onAssetInstallClick: () -> Unit
) {
    item {
        SettingsSubsection(strings.terminalTesting) {
            SettingsItem(
                title = strings.wipeTerminalEnv,
                subtitle = strings.wipeTerminalEnvDesc,
                onClick = onWipeClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }

    item {
        SettingsSubsection(strings.logging) {
            SettingsItem(
                title = strings.viewAppLogs,
                subtitle = strings.viewAppLogsDesc,
                onClick = onLogsClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }

    item {
        SettingsSubsection(strings.backupAndRestore) {
            SettingsItem(
                title = strings.exportSettings,
                subtitle = strings.exportSettingsDesc,
                onClick = onExportClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.FileUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )

            SettingsItem(
                title = strings.importSettings,
                subtitle = strings.importSettingsDesc,
                onClick = onImportClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }

    if (BuildConfig.DEBUG) {
        item {
            SettingsSubsection(strings.debugTesting) {
                SettingsItem(
                    title = strings.installBootstrapFromAssets,
                    subtitle = strings.installBootstrapFromAssetsDesc,
                    onClick = onAssetInstallClick,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Unarchive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AssetBootstrapConfirmationDialog(
    assetName: String,
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
                        imageVector = Icons.Rounded.Unarchive,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = strings.installFromAssetsQuestion,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = strings.installFromAssetsDesc(assetName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
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
                            text = strings.cancel,
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
                            text = strings.install,
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
private fun AssetInstallProgressDialog(label: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = strings.installingBootstrap,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
