package com.klyx.presentation.screen.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.fs.pluginsDir
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.ui.showFailureToast
import com.klyx.api.ui.theme.LocalIsDarkMode
import com.klyx.event.UiEvent
import com.klyx.network.fetchBody
import com.klyx.plugin.PluginManager
import com.klyx.plugin.PluginViewModel
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.PluginDetailPayload
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
import org.koin.compose.viewmodel.koinViewModel

private const val CDN = PluginManager.CDN
private const val API = PluginManager.API

private suspend fun fetchTextContent(id: String, fileName: String): String? =
    withContext(Dispatchers.IO) {
        val pluginDir = Paths.pluginsDir.resolve(id)
        try {
            fetchBody<String?>("$CDN/$id/$fileName")
        } catch (_: Exception) {
            runCatching { pluginDir.resolve(fileName).readText() }.getOrNull()
        }
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PluginDetailsScreen(payload: PluginDetailPayload) {
    val navigator = LocalNavigator.current
    val storeViewModel: PluginStoreViewModel = koinViewModel()
    val pluginViewModel: PluginViewModel = koinViewModel()

    val pluginUiState by pluginViewModel.uiState.collectAsStateWithLifecycle()
    val storeUiState by storeViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    var readme by remember { mutableStateOf<String?>(null) }
    var changelog by remember { mutableStateOf<String?>(null) }
    var loadingFiles by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(payload.id) {
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
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(payload.iconUrl ?: "$CDN/${payload.id}/icon.png")
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            placeholder = rememberVectorPainter(Icons.Outlined.Extension),
                            error = rememberVectorPainter(Icons.Outlined.Extension),
                            contentScale = ContentScale.Fit,
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = payload.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "by ${payload.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "v${payload.version}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                if (payload.downloadCount > 0) {
                                    Text(
                                        text = "${payload.downloadCount} dl",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (payload.description.isNotBlank()) {
                item {
                    Text(
                        text = payload.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }

            item {
                if (isPluginActuallyInstalled) {
                    val uninstalling = pluginUiState.isUnloading
                    Button(
                        onClick = { pluginViewModel.unloadPlugin(payload.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        enabled = !uninstalling
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
                    val installing by remember { derivedStateOf { storeUiState.installState != null } }
                    Button(
                        onClick = {
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !installing
                    ) {
                        if (installing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = storeUiState.installState?.message ?: "Installing...",
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Install", style = MaterialTheme.typography.labelLarge)
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
                    if (readme != null) add("Readme")
                    if (changelog != null) add("Changelog")
                }

                if (availableTabs.isNotEmpty()) {
                    item {
                        PrimaryTabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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

                    item {
                        val currentTabTitle = availableTabs.getOrNull(selectedTab)
                        val tabContent = if (currentTabTitle == "Readme") readme else changelog
                        val isDarkMode = LocalIsDarkMode.current
                        val highlightBuilder = remember(isDarkMode) {
                            Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkMode))
                        }

                        val state by produceState<State>(State.Loading(), tabContent) {
                            withContext(Dispatchers.Default) {
                                value = parseMarkdown(tabContent ?: "No content available")
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
                }
            }
        }
    }
}
