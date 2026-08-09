package com.klyx.presentation.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.api.plugin.PluginInfo
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.ui.showFailureToast
import com.klyx.app.icons.Archive
import com.klyx.app.icons.Download
import com.klyx.app.icons.Update
import com.klyx.event.UiEvent
import com.klyx.plugin.PluginUiState
import com.klyx.plugin.PluginViewModel
import com.klyx.presentation.components.ExpressiveMenuItem
import com.klyx.presentation.components.InstallationLogCard
import com.klyx.presentation.components.PluginIcon
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.PluginDetailPayload
import com.klyx.presentation.navigation.SettingsScreen
import com.klyx.presentation.screen.settings.components.SettingsSubsectionHeader
import com.klyx.presentation.viewmodel.PluginInstallState
import com.klyx.presentation.viewmodel.PluginStoreUiState
import com.klyx.presentation.viewmodel.PluginStoreViewModel
import com.klyx.presentation.viewmodel.StorePlugin
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PluginsScreen() {
    val toastHostState = LocalToastHostState.current
    val navigator = LocalNavigator.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showMenu by remember { mutableStateOf(false) }

    val viewModel: PluginViewModel = koinViewModel()
    val storeViewModel: PluginStoreViewModel = koinViewModel()

    val pluginUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val storeUiState by storeViewModel.uiState.collectAsStateWithLifecycle()

    val bundlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.loadPluginBundle(uri)
    }

    LaunchedEffect(Unit) {
        launch {
            viewModel.events.collect { event ->
                when (event) {
                    is UiEvent.ShowError -> toastHostState.showFailureToast(event.error)
                    is UiEvent.ShowMessage -> toastHostState.showToast(event.message)
                }
            }
        }

        launch {
            storeViewModel.events.collect { event ->
                when (event) {
                    is UiEvent.ShowError -> toastHostState.showFailureToast(event.error)
                    is UiEvent.ShowMessage -> toastHostState.showToast(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Plugins") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateBack() },
                        shapes = IconButtonDefaults.shapes(shape = CircleShape),
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
                    Box(modifier = Modifier.padding(end = 14.dp)) {
                        FilledIconButton(
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = { showMenu = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More Options"
                            )
                        }

                        MaterialTheme(
                            shapes = MaterialTheme.shapes.copy(
                                extraSmall = RoundedCornerShape(20.dp)
                            )
                        ) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.widthIn(min = 200.dp)
                            ) {
                                ExpressiveMenuItem(
                                    text = "Install from file",
                                    icon = Icons.Rounded.Archive,
                                    onClick = {
                                        showMenu = false
                                        bundlePickerLauncher.launch(
                                            arrayOf(
                                                "application/gzip",
                                                "application/x-gzip",
                                                "application/x-gtar",
                                                "application/octet-stream"
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pluginList(
                pluginUiState = pluginUiState,
                storeUiState = storeUiState,
                onUnload = { id -> viewModel.unloadPlugin(id) },
                onInstallStorePlugin = { plugin, onComplete ->
                    storeViewModel.installPlugin(plugin) {
                        viewModel.refresh()
                        onComplete()
                    }
                },
                onOpenInstalledDetail = { info ->
                    val payload = PluginDetailPayload(
                        id = info.descriptor.id,
                        name = info.descriptor.name,
                        version = info.descriptor.version,
                        description = info.descriptor.description,
                        author = info.descriptor.author?.name ?: "Unknown",
                        isInstalled = true,
                        iconUrl = info.iconPath,
                        sourceUri = viewModel.localBundleSource(info.descriptor.id)?.toString()
                    )
                    navigator.navigateTo(SettingsScreen.PluginDetail(payload))
                },
                onOpenStoreDetail = { plugin ->
                    val payload = PluginDetailPayload(
                        id = plugin.id,
                        name = plugin.name,
                        version = plugin.version,
                        description = plugin.description,
                        author = plugin.author,
                        isInstalled = false,
                        iconUrl = plugin.iconUrl,
                        downloadCount = plugin.downloadCount
                    )
                    navigator.navigateTo(SettingsScreen.PluginDetail(payload))
                }
            )
        }
    }
}

/** All items shown in the plugins [LazyColumn]. */
private fun LazyListScope.pluginList(
    pluginUiState: PluginUiState,
    storeUiState: PluginStoreUiState,
    onUnload: (String) -> Unit,
    onInstallStorePlugin: (StorePlugin, () -> Unit) -> Unit,
    onOpenInstalledDetail: (PluginInfo) -> Unit,
    onOpenStoreDetail: (StorePlugin) -> Unit
) {
    val installedIds = pluginUiState.plugins.map { it.descriptor.id }.toSet()
    val installed = pluginUiState.plugins
    val store = storeUiState.storePlugins.filterNot { it.id in installedIds }

    if (pluginUiState.loadingState != null) {
        item(key = "loading-bundle") {
            InstallationLogCard(
                title = "Installing Local Bundle",
                logs = pluginUiState.loadingState?.logs ?: emptyList()
            )
        }
    }

    if (installed.isEmpty() && store.isEmpty() && !storeUiState.storeLoading) {
        item(key = "empty") {
            Spacer(modifier = Modifier.height(64.dp))
            Text(
                text = "No plugins available.\nInstall a local bundle via the option menu.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    if (installed.isNotEmpty()) {
        item(key = "header-installed") {
            SettingsSubsectionHeader(title = "Installed")
        }
        items(installed, key = { "installed:${it.descriptor.id}" }) { info ->
            val storePlugin = storeUiState.storePlugins.find { it.id == info.descriptor.id }
            var operationRunning by remember { mutableStateOf(false) }

            InstalledPluginCard(
                plugin = info,
                storeVersion = storePlugin?.version,
                runningState = operationRunning,
                onUnload = {
                    operationRunning = true
                    onUnload(info.descriptor.id)
                },
                onUpdate = {
                    if (storePlugin != null) {
                        operationRunning = true
                        onInstallStorePlugin(storePlugin) {
                            operationRunning = false
                        }
                    }
                },
                onDetail = { onOpenInstalledDetail(info) }
            )
        }
    }

    if (store.isNotEmpty()) {
        item(key = "header-store") {
            SettingsSubsectionHeader(title = "Store")
        }
        items(store, key = { "store:${it.id}" }) { plugin ->
            val installState = storeUiState.installState
            val isInstalling = installState?.plugin?.id == plugin.id

            StorePluginCard(
                plugin = plugin,
                installState = installState.takeIf { isInstalling },
                onInstall = { onInstallStorePlugin(plugin) {} },
                onDetail = { onOpenStoreDetail(plugin) }
            )

            AnimatedVisibility(
                visible = isInstalling,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (installState != null) {
                    InstallationLogCard(
                        title = "Installing ${plugin.name}",
                        logs = installState.logs
                    )
                }
            }
        }
    }

    if (storeUiState.storeLoading) {
        item(key = "loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InstalledPluginCard(
    plugin: PluginInfo,
    storeVersion: String?,
    runningState: Boolean,
    onUnload: () -> Unit,
    onUpdate: () -> Unit,
    onDetail: () -> Unit,
) {
    val desc = plugin.descriptor
    val updateAvailable = storeVersion != null && storeVersion != desc.version

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onDetail() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PluginIcon(model = plugin.iconPath)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = desc.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "v${desc.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (desc.description.isNotBlank()) {
                    Text(
                        text = desc.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (updateAvailable) {
                Button(
                    onClick = onUpdate,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 32.dp)
                ) {
                    if (runningState) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    } else {
                        Icon(Icons.Rounded.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                FilledIconButton(
                    onClick = onUnload,
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    if (runningState) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        Icon(Icons.Rounded.Delete, contentDescription = "Uninstall", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StorePluginCard(
    plugin: StorePlugin,
    installState: PluginInstallState?,
    onInstall: () -> Unit,
    onDetail: () -> Unit,
) {
    val installing = installState != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onDetail() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                PluginIcon(model = plugin.iconUrl)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "v${plugin.version} • by ${plugin.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onInstall,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 32.dp)
                ) {
                    if (installing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (plugin.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }

            if (installing) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = installState.message ?: "Installing...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                val progress = installState.progress
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }
        }
    }
}
