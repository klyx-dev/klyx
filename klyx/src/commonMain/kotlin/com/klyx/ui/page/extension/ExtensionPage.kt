@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.klyx.ui.page.extension

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klyx.LocalNavigator
import com.klyx.NavigationScope
import com.klyx.Navigator
import com.klyx.core.LocalPlatformContext
import com.klyx.core.PlatformContext
import com.klyx.core.app.globalOf
import com.klyx.core.io.Paths
import com.klyx.core.io.extensionsDir
import com.klyx.core.ui.component.BackButton
import com.klyx.core.util.join
import com.klyx.extension.nodegraph.Extension
import com.klyx.extension.nodegraph.ExtensionManager
import com.klyx.extension.nodegraph.ExtensionMetadata
import com.klyx.icons.Add
import com.klyx.icons.Edit
import com.klyx.icons.Icons
import com.klyx.icons.Info
import com.klyx.icons.Visibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlin.time.Clock

@Composable
context(navigationScope: NavigationScope)
fun ExtensionPage(modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val store = remember { ExtensionStore() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Installed", "Store")

    val context = LocalPlatformContext.current
    val navigator = LocalNavigator.current

    val extensionManager = globalOf<ExtensionManager>()

    val storeExtensions = remember { mutableStateListOf<StoreExtension>() }
    val localPublishedExtensions = remember { mutableStateListOf<StoreExtension>() }
    var isStoreLoading by remember { mutableStateOf(true) }

    suspend fun refreshStore() {
        isStoreLoading = true
        storeExtensions.clear()
        storeExtensions.addAll(store.fetchStoreIndex())
        localPublishedExtensions.removeAll { it in storeExtensions }
        isStoreLoading = false
    }

    LaunchedEffect(Unit) {
        refreshStore()
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = TopAppBarDefaults.topAppBarColors()

    val containerColor by animateColorAsState(
        targetValue = lerp(
            colors.containerColor,
            colors.scrolledContainerColor,
            scrollBehavior.state.collapsedFraction
        )
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                LargeTopAppBar(
                    title = { Text("Extensions") },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = containerColor,
                        scrolledContainerColor = containerColor
                    ),
                    actions = {
                        val tooltipState = rememberTooltipState()

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Start),
                            tooltip = {
                                PlainTooltip { Text("Extensions are experimental.") }
                            },
                            state = tooltipState
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        tooltipState.show()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Info,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    navigationIcon = { BackButton(navigationScope.navigator::navigateBack) }
                )

                SecondaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = containerColor
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val destDir = Paths.extensionsDir.join("local")
                        val newPath = destDir.join("extension_${Clock.System.now().toEpochMilliseconds()}.kxext")
                        editOrViewExtension(context, navigator, edit = true, newPath.toString())
                    },
                    text = { Text("New Extension") },
                    icon = { Icon(Icons.Add, contentDescription = null) }
                )
            }
        }
    ) { innerPadding ->

        fun openExtension(filePath: Path, edit: Boolean) {
            editOrViewExtension(context, navigator, edit, filePath.toString())
        }

        val extensions by extensionManager.extensions.collectAsState()
        val isExtensionLoading by extensionManager.isLoading.collectAsState()

        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (selectedTabIndex == 0) {
                if (extensions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isExtensionLoading) {
                            CircularWavyProgressIndicator(trackColor = Color.Transparent)
                        } else {
                            Text("No extensions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                    ) {
                        items(items = extensions) { extension ->
                            val metadata = extension.metadata
                            val remoteStoreMatch = storeExtensions.find { it.id == metadata.id }
                                ?: localPublishedExtensions.find { it.id == metadata.id }

                            ExtensionItem(
                                metadata = metadata,
                                isLocal = extension.isLocal,
                                storeExt = remoteStoreMatch,
                                onClick = { openExtension(extension.filePath, edit = false) },
                                onEdit = { openExtension(extension.filePath, edit = true) },
                                onView = { openExtension(extension.filePath, edit = false) },
                                canPublish = !metadata.id.startsWith("broken."),
                                onPublish = {
                                    val errorMessage =
                                        verifyMetadataForPublish(metadata, extension.filePath, extensions)

                                    if (errorMessage != null) {
                                        snackbarHostState.showSnackbar(errorMessage)
                                    } else {
                                        val success = store.publishExtension(extension.filePath, metadata)
                                        if (success) {
                                            snackbarHostState.showSnackbar(if (remoteStoreMatch != null) "Updated successfully!" else "Published successfully!")

                                            val generatedDownloadUrl =
                                                "https://cdn.jsdelivr.net/gh/klyx-dev/extensions@main/binaries/${metadata.id}_v${metadata.version}.kxext"
                                            val updatedExt = StoreExtension(
                                                id = metadata.id,
                                                name = metadata.name,
                                                author = metadata.author,
                                                version = metadata.version,
                                                description = metadata.description,
                                                downloadUrl = remoteStoreMatch?.downloadUrl ?: generatedDownloadUrl
                                            )

                                            val index = storeExtensions.indexOfFirst { it.id == metadata.id }
                                            if (index >= 0) {
                                                storeExtensions[index] = updatedExt
                                            } else {
                                                localPublishedExtensions.add(updatedExt)
                                            }
                                        } else {
                                            snackbarHostState.showSnackbar("Publish failed. Check logs!")
                                        }
                                    }
                                },
                                onUnpublish = {
                                    val success = store.unpublishExtension(metadata.id)
                                    if (success) {
                                        snackbarHostState.showSnackbar("Removed from store successfully!")
                                        storeExtensions.removeAll { it.id == metadata.id }
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to unpublish. Check logs!")
                                    }
                                },
                                onDelete = {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            extensionManager.removeExtension(extension)
                                            snackbarHostState.showSnackbar("${metadata.name} deleted.")
                                        } catch (_: Exception) {
                                            snackbarHostState.showSnackbar("Failed to delete ${metadata.name}.")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                if (storeExtensions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isStoreLoading) {
                            CircularWavyProgressIndicator(trackColor = Color.Transparent)
                        } else {
                            Text(
                                "No extensions found in the store.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val ptrState = rememberPullToRefreshState()

                    PullToRefreshBox(
                        state = ptrState,
                        isRefreshing = isStoreLoading,
                        onRefresh = { scope.launch { refreshStore() } },
                        indicator = {
                            PullToRefreshDefaults.IndicatorBox(
                                state = ptrState,
                                modifier = Modifier.align(Alignment.TopCenter),
                                isRefreshing = isStoreLoading,
                                maxDistance = PullToRefreshDefaults.IndicatorMaxDistance
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(storeExtensions + localPublishedExtensions) { storeExt ->
                                val isInstalled = extensions.any { it.metadata.id == storeExt.id }

                                StoreExtensionItem(
                                    storeExt = storeExt,
                                    isInstalled = isInstalled,
                                    onInstall = {
                                        val destDir = Paths.extensionsDir
                                        val installedPath = store.downloadExtension(storeExt, destDir)
                                        if (installedPath != null) {
                                            extensionManager.addOrReplaceExtension(installedPath, isLocal = false)
                                            snackbarHostState.showSnackbar("${storeExt.name} Installed!")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to install ${storeExt.name}.")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionItem(
    metadata: ExtensionMetadata,
    isLocal: Boolean,
    storeExt: StoreExtension?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onView: () -> Unit,
    canPublish: Boolean,
    onPublish: suspend () -> Unit,
    onUnpublish: suspend () -> Unit,
    onDelete: () -> Unit,
) {
    var isPublishing by remember { mutableStateOf(false) }
    var isUnpublishing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val isBusy = isPublishing || isUnpublishing

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "by ${metadata.author}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isLocal) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Edit, contentDescription = "Edit")
                    }
                } else {
                    IconButton(onClick = onView) {
                        Icon(Icons.Visibility, contentDescription = "View")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (metadata.description.isNotBlank()) {
                Text(
                    text = metadata.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetaChip("v${metadata.version}")

                    if (metadata.supportedLanguages.isNotEmpty()) {
                        MetaChip(metadata.supportedLanguages.joinToString(", "))
                    }

                    if (isLocal) MetaChip("Local")
                }

                if (isLocal) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        enabled = !isBusy
                    ) {
                        Text("Delete")
                    }

                    val isPublished = storeExt != null
                    val hasUpdate = isPublished && storeExt.version != metadata.version

                    if (isPublished) {
                        TextButton(
                            onClick = {
                                isUnpublishing = true
                                scope.launch {
                                    onUnpublish()
                                    isUnpublishing = false
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            enabled = !isBusy
                        ) {
                            if (isUnpublishing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("Unpublish")
                        }
                    }

                    if (!isPublished || hasUpdate) {
                        Button(
                            onClick = {
                                isPublishing = true
                                scope.launch {
                                    onPublish()
                                    isPublishing = false
                                }
                            },
                            enabled = !isBusy && canPublish,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (hasUpdate) "Update" else "Publish", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "Uninstall",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreExtensionItem(
    storeExt: StoreExtension,
    isInstalled: Boolean,
    onInstall: suspend () -> Unit
) {
    var isInstalling by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = storeExt.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "by ${storeExt.author}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = storeExt.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaChip("v${storeExt.version}")
                Spacer(Modifier.weight(1f))

                if (isInstalled) {
                    TextButton(
                        onClick = { },
                        enabled = false,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Installed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Button(
                        onClick = {
                            isInstalling = true
                            scope.launch {
                                onInstall()
                                isInstalling = false
                            }
                        },
                        enabled = !isInstalling,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isInstalling) "Installing..." else "Install")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal expect fun editOrViewExtension(context: PlatformContext, navigator: Navigator, edit: Boolean, filePath: String)

private fun verifyMetadataForPublish(
    metadata: ExtensionMetadata,
    filePath: Path,
    allExtensions: List<Extension>
): String? {
    if (metadata.id.startsWith("broken.")) {
        return "Cannot publish: You must configure the ExtensionConfig variable in the editor."
    }

    if (metadata.name.isBlank()) return "Publish failed: Name cannot be blank."
    if (metadata.author.isBlank()) return "Publish failed: Author cannot be blank."
    if (metadata.version.isBlank()) return "Publish failed: Version cannot be blank."
    if (metadata.description.isBlank()) return "Publish failed: Description cannot be blank."

    val idRegex = "^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$".toRegex()
    if (!metadata.id.matches(idRegex)) {
        return "Publish failed: ID must be a valid package format (e.g., com.yourname.extension)."
    }

    val collision = allExtensions.find {
        it.metadata.id == metadata.id && it.filePath.name != filePath.name
    }

    if (collision != null) {
        return "Publish failed: ID '${metadata.id}' is already being used by the file '${collision.filePath.name}'."
    }

    return null
}
