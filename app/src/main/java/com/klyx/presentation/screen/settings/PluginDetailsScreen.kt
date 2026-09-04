package com.klyx.presentation.screen.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.api.plugin.PluginDescriptor
import com.klyx.api.plugin.PluginSettingsRegistry
import com.klyx.api.service.Logger
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.ui.showFailureToast
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.event.UiEvent
import com.klyx.i18n.strings
import com.klyx.plugin.PluginViewModel
import com.klyx.presentation.components.LogEntryItem
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.PluginDetailPayload
import com.klyx.presentation.navigation.PluginSettingsPayload
import com.klyx.presentation.navigation.SettingsScreen
import com.klyx.presentation.screen.settings.plugin.PluginAboutSheet
import com.klyx.presentation.screen.settings.plugin.PluginDetailsSkeleton
import com.klyx.presentation.screen.settings.plugin.PluginDetailsTabs
import com.klyx.presentation.screen.settings.plugin.PluginHeroCard
import com.klyx.presentation.screen.settings.plugin.PluginInstallButton
import com.klyx.presentation.screen.settings.plugin.PluginLoadError
import com.klyx.presentation.screen.settings.plugin.PluginMarkdownContent
import com.klyx.presentation.screen.settings.plugin.availableTabs
import com.klyx.presentation.screen.settings.plugin.fetchPluginDescriptor
import com.klyx.presentation.screen.settings.plugin.fetchTextContent
import com.klyx.presentation.viewmodel.PluginStoreViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
    UnsafeGlobalAccess::class
)
@Composable
fun PluginDetailsScreen(payload: PluginDetailPayload) {
    val navigator = LocalNavigator.current
    val storeViewModel: PluginStoreViewModel = koinViewModel()
    val pluginViewModel: PluginViewModel = koinViewModel()
    val logger: Logger = koinInject()

    val pluginUiState by pluginViewModel.uiState.collectAsStateWithLifecycle()
    val storeUiState by storeViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var descriptor by remember { mutableStateOf<PluginDescriptor?>(null) }
    var readme by remember { mutableStateOf<String?>(null) }
    var changelog by remember { mutableStateOf<String?>(null) }
    var loadingFiles by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var reinstalling by remember { mutableStateOf(false) }

    LaunchedEffect(payload.id, reloadKey) {
        loadingFiles = true
        loadError = false
        val fetchedDescriptor = fetchPluginDescriptor(payload.id)
        val fetchedReadme = fetchTextContent(payload.id, "readme.md")
        val fetchedChangelog = fetchTextContent(payload.id, "changelog.md")
        descriptor = fetchedDescriptor
        readme = fetchedReadme
        changelog = fetchedChangelog

        loadError = fetchedDescriptor == null && fetchedReadme == null && fetchedChangelog == null
        loadingFiles = false
    }

    val toastHostState = LocalToastHostState.current
    LaunchedEffect(Unit) {
        launch {
            storeViewModel.events.collect { event ->
                when (event) {
                    is UiEvent.ShowError -> toastHostState.showFailureToast(event.error)
                    is UiEvent.ShowMessage -> toastHostState.showToast(event.message)
                }
            }
        }
    }

    val isPluginActuallyInstalled by remember(pluginUiState.plugins, payload.id) {
        derivedStateOf {
            pluginUiState.plugins.any { it.descriptor.id == payload.id }
        }
    }

    val settingsRegistry: PluginSettingsRegistry = GlobalApp.global()
    val hasPluginSettings = settingsRegistry.hasSettings(payload.id)

    val allLogs by logger.entries.collectAsState()
    val pluginLogs by remember(allLogs, payload.id) {
        derivedStateOf {
            allLogs.filter { it.sourcePluginId == payload.id }
        }
    }

    val logTimeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = payload.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateBack() },
                        shape = CircleShape,
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
                },
                actions = {
                    if (isPluginActuallyInstalled && hasPluginSettings) {
                        FilledIconButton(
                            modifier = Modifier.padding(end = 12.dp, top = 4.dp),
                            onClick = {
                                navigator.navigateTo(
                                    SettingsScreen.PluginSettings(
                                        PluginSettingsPayload(
                                            id = payload.id,
                                            name = payload.name,
                                            iconUrl = payload.iconUrl
                                        )
                                    )
                                )
                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = strings.settings
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 3.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    PluginInstallButton(
                        payload = payload,
                        isPluginActuallyInstalled = isPluginActuallyInstalled,
                        pluginUiState = pluginUiState,
                        storeUiState = storeUiState,
                        onUninstall = { pluginViewModel.unloadPlugin(payload.id) },
                        onBundleSourceExists = { uri -> pluginViewModel.bundleSourceExists(uri) },
                        onInstallFromBundle = {
                            reinstalling = true
                            pluginViewModel.loadPluginBundle(it) {
                                reinstalling = false
                            }
                        },
                        onInstallFromStore = {
                            storeViewModel.installPlugin(it) {
                                pluginViewModel.refresh()
                            }
                        },
                        onNavigateBack = { navigator.navigateBack() },
                        reinstalling = reinstalling,
                        onReinstallingChange = { reinstalling = it }
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PluginHeroCard(
                    payload = payload,
                    descriptor = descriptor,
                    onAuthorClick = { showAboutSheet = true }
                )
            }

            when {
                loadingFiles -> item { PluginDetailsSkeleton() }

                loadError -> item {
                    PluginLoadError(onRetry = { reloadKey++ })
                }

                else -> {
                    val tabs = availableTabs(changelog, pluginLogs)
                    if (tabs.size > 1) {
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(vertical = 4.dp)
                            ) {
                                PluginDetailsTabs(
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it },
                                    pluginLogs = pluginLogs,
                                    changelog = changelog
                                )
                            }
                        }
                    }

                    when (tabs.getOrNull(selectedTab)) {
                        "Logs" -> {
                            items(pluginLogs.reversed(), key = { "${it.timestamp}_${it.hashCode()}" }) { entry ->
                                LogEntryItem(entry = entry, timeFormat = logTimeFormat)
                            }
                        }

                        "Changelog" -> {
                            item {
                                PluginMarkdownContent(
                                    content = changelog,
                                    emptyText = strings.noChangelogProvided
                                )
                            }
                        }

                        else -> {
                            item {
                                PluginMarkdownContent(
                                    content = readme,
                                    emptyText = strings.noDetailsProvided
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAboutSheet) {
        PluginAboutSheet(
            payload = payload,
            descriptor = descriptor,
            onDismiss = { showAboutSheet = false }
        )
    }
}
