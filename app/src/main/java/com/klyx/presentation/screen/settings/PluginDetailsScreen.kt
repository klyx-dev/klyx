package com.klyx.presentation.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.fs.pluginsDir
import com.klyx.api.plugin.PluginDescriptor
import com.klyx.api.plugin.PluginSettingsRegistry
import com.klyx.api.service.Logger
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.ui.showFailureToast
import com.klyx.api.ui.theme.LocalIsDarkMode
import com.klyx.api.util.openUrl
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.event.UiEvent
import com.klyx.network.fetchBody
import com.klyx.plugin.PluginManager
import com.klyx.plugin.PluginViewModel
import com.klyx.presentation.components.InstallationLogCard
import com.klyx.presentation.components.LogEntryItem
import com.klyx.presentation.components.PluginIcon
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.PluginDetailPayload
import com.klyx.presentation.navigation.PluginSettingsPayload
import com.klyx.presentation.navigation.SettingsScreen
import com.klyx.presentation.viewmodel.PluginStoreViewModel
import com.klyx.presentation.viewmodel.StorePlugin
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.extendedspans.ExtendedSpans
import com.mikepenz.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.mikepenz.markdown.compose.extendedspans.SquigglyUnderlineSpanPainter
import com.mikepenz.markdown.compose.extendedspans.rememberSquigglyUnderlineAnimator
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.parseMarkdown
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.text.SimpleDateFormat
import java.util.Locale

private const val CDN = PluginManager.CDN
private const val API = PluginManager.API

private val pluginJson = Json { ignoreUnknownKeys = true }

private suspend fun fetchTextContent(id: String, fileName: String): String? =
    withContext(Dispatchers.IO) {
        val pluginDir = Paths.pluginsDir.resolve(id)
        val localFile = pluginDir.resolve(fileName)
        val localText = runCatching { localFile.readText().takeIf { it.isNotBlank() } }.getOrNull()
        localText ?: try {
            fetchBody<String?>("$CDN/$id/$fileName")
        } catch (_: Exception) {
            null
        }
    }

private suspend fun fetchPluginDescriptor(id: String): PluginDescriptor? =
    withContext(Dispatchers.IO) {
        val pluginDir = Paths.pluginsDir.resolve(id)
        val localJson = pluginDir.resolve("plugin.json")
        if (localJson.exists()) {
            runCatching {
                pluginJson.decodeFromString<PluginDescriptor>(localJson.readText())
            }.getOrNull()
        } else {
            try {
                fetchBody("$CDN/$id/plugin.json")
            } catch (_: Exception) {
                null
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, UnsafeGlobalAccess::class)
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
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var reinstalling by remember { mutableStateOf(false) }

    LaunchedEffect(payload.id) {
        descriptor = fetchPluginDescriptor(payload.id)
        readme = fetchTextContent(payload.id, "readme.md")
        changelog = fetchTextContent(payload.id, "changelog.md")
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
                            contentDescription = "Back"
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
                                contentDescription = "Settings"
                            )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PluginHeroCard(
                    payload = payload,
                    descriptor = descriptor,
                    onAuthorClick = { showAboutSheet = true }
                )
            }

            item {
                if (isPluginActuallyInstalled) {
                    val uninstalling = pluginUiState.isUnloading
                    Button(
                        onClick = { pluginViewModel.unloadPlugin(payload.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        if (uninstalling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uninstalling...", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uninstall", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    val installState = storeUiState.installState
                    val isThisInstalling = installState?.plugin?.id == payload.id
                    val installing by remember { derivedStateOf { storeUiState.installState != null } }

                    Column {
                        Button(
                            onClick = {
                                val localSource = payload.sourceUri?.takeIf { it.isNotBlank() }
                                if (localSource != null) {
                                    val uri = localSource.toUri()
                                    if (pluginViewModel.bundleSourceExists(uri)) {
                                        reinstalling = true
                                        pluginViewModel.loadPluginBundle(uri) {
                                            reinstalling = false
                                        }
                                    } else {
                                        navigator.navigateBack()
                                    }
                                } else {
                                    storeViewModel.installPlugin(
                                        plugin = StorePlugin(
                                            id = payload.id,
                                            name = payload.name,
                                            description = payload.description,
                                            author = payload.author,
                                            version = payload.version,
                                            minAppVersion = "",
                                            maxAppVersion = null,
                                            downloadCount = payload.downloadCount,
                                            iconUrl = payload.iconUrl ?: "$CDN/${payload.id}/icon.png",
                                            downloadUrl = "$API/dl/${payload.id}/${payload.version}"
                                        )
                                    ) {
                                        pluginViewModel.refresh()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = (!installing && !reinstalling) || isThisInstalling || reinstalling
                        ) {
                            when {
                                isThisInstalling -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = installState.message ?: "Installing...",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                reinstalling -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Installing...",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                else -> {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (installing) "Another task running" else "Install",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isThisInstalling,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (installState != null) {
                                    if (installState.message != null) {
                                        Text(
                                            text = installState.message,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                    if (installState.progress > 0f) {
                                        LinearProgressIndicator(
                                            progress = { installState.progress.coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                        )
                                    }
                                }
                                if (installState != null) {
                                    InstallationLogCard(
                                        title = "Installation Logs",
                                        logs = installState.logs
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (loadingFiles) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
            } else {
                val availableTabs = buildList {
                    add("Details")
                    if (!changelog.isNullOrBlank()) add("Changelog")
                    if (pluginLogs.isNotEmpty()) add("Logs")
                }

                if (availableTabs.size > 1) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                PrimaryTabRow(
                                    selectedTabIndex = selectedTab.coerceIn(0, availableTabs.size - 1),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    divider = { }
                                ) {
                                    availableTabs.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTab == index,
                                            onClick = { selectedTab = index },
                                            text = { Text(title, fontWeight = FontWeight.Bold) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val currentTabTitle = availableTabs.getOrNull(selectedTab)

                when (currentTabTitle) {
                    "Logs" -> {
                        items(pluginLogs.reversed(), key = { "${it.timestamp}_${it.hashCode()}" }) { entry ->
                            LogEntryItem(entry = entry, timeFormat = logTimeFormat)
                        }
                    }

                    "Changelog" -> {
                        item {
                            PluginMarkdownContent(
                                content = changelog,
                                emptyText = "No changelog provided for this plugin."
                            )
                        }
                    }

                    else -> {
                        item {
                            PluginMarkdownContent(
                                content = readme,
                                emptyText = "No details provided for this plugin."
                            )
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

@Composable
private fun PluginMarkdownContent(
    content: String?,
    emptyText: String,
) {
    if (content.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val isDarkMode = LocalIsDarkMode.current
    val highlightBuilder = remember(isDarkMode) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkMode))
    }

    val state by produceState<State>(State.Loading(), content) {
        withContext(Dispatchers.Default) {
            value = parseMarkdown(content)
        }
    }

    Markdown(
        state = state,
        extendedSpans = markdownExtendedSpans {
            val animator = rememberSquigglyUnderlineAnimator()
            remember {
                ExtendedSpans(
                    RoundedCornerSpanPainter(),
                    SquigglyUnderlineSpanPainter(animator = animator)
                )
            }
        },
        imageTransformer = Coil3ImageTransformerImpl,
        components = markdownComponents(
            codeBlock = {
                MarkdownHighlightedCodeBlock(
                    content = it.content,
                    node = it.node,
                    highlightsBuilder = highlightBuilder,
                    showHeader = true
                )
            },
            codeFence = {
                MarkdownHighlightedCodeFence(
                    content = it.content,
                    node = it.node,
                    highlightsBuilder = highlightBuilder,
                    showHeader = true
                )
            }
        )
    )
}

@Composable
private fun PluginHeroCard(
    payload: PluginDetailPayload,
    descriptor: PluginDescriptor?,
    onAuthorClick: () -> Unit,
) {
    val heroShape = AbsoluteSmoothCornerShape(30.dp, 30)
    val author = descriptor?.author?.name ?: payload.author
    val github = descriptor?.author?.github

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = heroShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PluginIcon(
                        model = payload.iconUrl ?: "$CDN/${payload.id}/icon.png",
                        size = 60.dp,
                        cornerRadius = 16.dp,
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = payload.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        val authorShape = RoundedCornerShape(8.dp)
                        val authorModifier = Modifier
                            .clip(authorShape)
                            .clickable(onClick = onAuthorClick)

                        Surface(
                            modifier = authorModifier,
                            shape = authorShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.25f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "by $author",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!github.isNullOrBlank()) {
                                    Text(
                                        text = " @$github",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "View author details",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = "v${payload.version}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }

                    if (payload.downloadCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "${payload.downloadCount}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    val license = descriptor?.license
                    if (!license.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = license,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (payload.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = payload.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginAboutSheet(
    payload: PluginDetailPayload,
    descriptor: PluginDescriptor?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded)
    )
    val coroutineScope = rememberCoroutineScope()
    val author = descriptor?.author
    val authorName = author?.name ?: payload.author
    val github = author?.github
    val email = author?.email
    val website = author?.url
    val minAppVersion = descriptor?.minAppVersion
    val license = descriptor?.license
    val links = descriptor?.links

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PluginIcon(
                    model = payload.iconUrl ?: "$CDN/${payload.id}/icon.png",
                    size = 52.dp,
                    cornerRadius = 14.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payload.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "v${payload.version}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }
                            .invokeOnCompletion { onDismiss() }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp),
                    )
                }
            }

            Surface(
                shape = AbsoluteSmoothCornerShape(22.dp, 60),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        AuthorAvatar(name = authorName)

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Author",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = authorName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (!github.isNullOrBlank()) {
                            Surface(
                                onClick = { openUrl(githubUrl(github)) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AlternateEmail,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "@$github",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    if (!email.isNullOrBlank() || !website.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (!email.isNullOrBlank()) {
                                ContactPill(
                                    icon = Icons.Rounded.Mail,
                                    text = email,
                                    onClick = { openUrl("mailto:$email") },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (!website.isNullOrBlank()) {
                                ContactPill(
                                    icon = Icons.Rounded.Link,
                                    text = website,
                                    onClick = { openUrl(website) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PluginStatTile(
                    label = "Version",
                    value = "v${payload.version}",
                    modifier = Modifier.weight(1f),
                )
                if (payload.downloadCount > 0) {
                    PluginStatTile(
                        label = "Downloads",
                        value = "${payload.downloadCount}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (!minAppVersion.isNullOrBlank() || !license.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (!minAppVersion.isNullOrBlank()) {
                        PluginStatTile(
                            label = "Requires App",
                            value = "v$minAppVersion",
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (!license.isNullOrBlank()) {
                        PluginStatTile(
                            label = "License",
                            value = license,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (links != null) {
                val linkItems = buildList {
                    links.source?.let { add(SheetLinkItem("Source", it, Icons.Rounded.Code)) }
                    links.issues?.let { add(SheetLinkItem("Issues", it, Icons.Rounded.BugReport)) }
                    links.website?.let { add(SheetLinkItem("Website", it, Icons.Rounded.Public)) }
                    links.donate?.let { add(SheetLinkItem("Donate", it, Icons.Rounded.Favorite)) }
                }

                if (linkItems.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Links",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        Surface(
                            shape = AbsoluteSmoothCornerShape(22.dp, 60),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                linkItems.forEachIndexed { index, item ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        )
                                    }
                                    SheetLinkRow(item = item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorAvatar(
    name: String,
    modifier: Modifier = Modifier.size(56.dp),
) {
    val initials = remember(name) {
        name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifBlank { "?" }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer,
                    )
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun ContactPill(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class SheetLinkItem(
    val label: String,
    val url: String,
    val icon: ImageVector,
)

@Composable
private fun SheetLinkRow(item: SheetLinkItem) {
    Surface(
        onClick = { openUrl(item.url) },
        shape = RectangleShape,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp),
                )
            }
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = item.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.4f),
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PluginStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = AbsoluteSmoothCornerShape(14.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun githubUrl(github: String): String =
    if (github.startsWith("http")) github else "https://github.com/$github"
