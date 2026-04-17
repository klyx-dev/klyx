@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.klyx.ui.page.extension

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klyx.LocalNavigator
import com.klyx.NavigationScope
import com.klyx.Navigator
import com.klyx.core.LocalPlatformContext
import com.klyx.core.PlatformContext
import com.klyx.core.app.IdentityManager
import com.klyx.core.app.globalOf
import com.klyx.core.io.Paths
import com.klyx.core.io.extensionsDir
import com.klyx.core.ui.component.BackButton
import com.klyx.core.util.join
import com.klyx.extension.nodegraph.Extension
import com.klyx.extension.nodegraph.ExtensionManager
import com.klyx.extension.nodegraph.ExtensionMetadata
import com.klyx.extension.nodegraph.onInstall
import com.klyx.extension.nodegraph.onUninstall
import com.klyx.icons.Add
import com.klyx.icons.Edit
import com.klyx.icons.Icons
import com.klyx.icons.Info
import com.klyx.icons.Visibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.files.Path
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

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
    val localDeviceId = remember { IdentityManager.deviceId }

    val extensionManager = globalOf<ExtensionManager>()

    val storeExtensions = remember { mutableStateListOf<StoreExtension>() }
    val localPublishedExtensions = remember { mutableStateListOf<StoreExtension>() }
    var isStoreLoading by remember { mutableStateOf(true) }

    suspend fun refreshStore() = coroutineScope {
        isStoreLoading = true
        storeExtensions.clear()

        val storeIndexTask = async(Dispatchers.IO) { store.fetchStoreIndex() }
        val downloadCountsTask = async(Dispatchers.IO) { store.fetchDownloadCounts() }

        val remoteExtensions = storeIndexTask.await()
        val liveCounts = downloadCountsTask.await()

        val sorted = remoteExtensions.map { ext ->
            ext.copy(downloadCount = liveCounts[ext.id] ?: 0)
        }.sortedByDescending { it.downloadCount }

        storeExtensions.addAll(sorted)
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

    suspend fun unpublish(extensionId: String, publisherId: String) {
        val success = store.unpublishExtension(extensionId, publisherId)
        if (success) {
            snackbarHostState.showSnackbar("Removed from store successfully!")
            storeExtensions.removeAll { it.id == extensionId }
        } else {
            snackbarHostState.showSnackbar("Failed to unpublish. Check logs!")
        }
    }

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
                                isStoreLoading = isStoreLoading,
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
                                        val success =
                                            store.publishExtension(extension.filePath, metadata, localDeviceId)
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
                                                downloadUrl = remoteStoreMatch?.downloadUrl ?: generatedDownloadUrl,
                                                publisherId = localDeviceId
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
                                onUnpublish = { unpublish(metadata.id, localDeviceId) },
                                onDelete = {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            withTimeoutOrNull(1.minutes) {
                                                extensionManager.onUninstall(metadata.id)
                                            }

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
                                val localExt = extensions.find { it.metadata.id == storeExt.id }
                                val isInstalled = localExt != null

                                val hasUpdate = isInstalled && localExt.metadata.version != storeExt.version

                                StoreExtensionItem(
                                    storeExt = storeExt,
                                    isInstalled = isInstalled,
                                    hasUpdate = hasUpdate,
                                    localDeviceId = localDeviceId,
                                    onUnpublish = { unpublish(storeExt.id, localDeviceId) },
                                    onInstall = {
                                        val destDir = Paths.extensionsDir
                                        val installedPath = store.downloadExtension(storeExt, destDir)

                                        if (installedPath != null) {
                                            extensionManager.addOrReplaceExtension(installedPath, isLocal = false)
                                            snackbarHostState.showSnackbar("${storeExt.name} Installed!")

                                            val index = storeExtensions.indexOfFirst { it.id == storeExt.id }
                                            if (index >= 0) {
                                                storeExtensions[index] =
                                                    storeExt.copy(downloadCount = storeExt.downloadCount + 1)
                                            }

                                            scope.launch(Dispatchers.IO) {
                                                withTimeoutOrNull(5.minutes) {
                                                    extensionManager.onInstall(storeExt.id)
                                                }
                                            }
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
    isStoreLoading: Boolean,
) {
    var isPublishing by remember { mutableStateOf(false) }
    var isUnpublishing by remember { mutableStateOf(false) }

    var showLegacyWarning by remember { mutableStateOf(false) }
    var showPublishWarning by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val isBusy = isPublishing || isUnpublishing

    if (showLegacyWarning) {
        AlertDialog(
            onDismissRequest = { showLegacyWarning = false },
            title = { Text("Warning") },
            text = { Text("This extension is published on the store, but it was uploaded before the ownership system existed. If you delete it locally, you will never be able to update or unpublish it. Are you sure you want to delete?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLegacyWarning = false
                        onDelete()
                    }
                ) {
                    Text("Delete Anyway", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLegacyWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPublishWarning) {
        AlertDialog(
            onDismissRequest = { showPublishWarning = false },
            title = { Text("Publishing Notice") },
            text = {
                Column {
                    Text(
                        "Klyx uses anonymous, device-bound accounts. Your publisher identity is tied exclusively to this specific app installation.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "If you delete Klyx or clear its app data, you will lose the ability to update or unpublish this extension from within the app.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "If you lose access, you will need to manually open a Pull Request on our GitHub repository to remove it:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "github.com/klyx-dev/extensions",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                uriHandler.openUri("https://github.com/klyx-dev/extensions/blob/main/index.json")
                            }
                            .padding(4.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPublishWarning = false
                    isPublishing = true
                    scope.launch {
                        onPublish()
                        isPublishing = false
                    }
                }) {
                    Text("Understood, Publish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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

                    if (storeExt != null) {
                        Text(
                            text = "${storeExt.downloadCount} Downloads",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                if (isLocal) {
                    TextButton(
                        onClick = {
                            val isPublished = storeExt != null
                            val hasNoOwner = storeExt?.publisherId.isNullOrEmpty()

                            if (isPublished && hasNoOwner) {
                                showLegacyWarning = true
                            } else {
                                onDelete()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        enabled = !isBusy
                    ) {
                        Text("Delete")
                    }

                    if (isStoreLoading) {
                        Button(
                            onClick = { },
                            enabled = false,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Checking...", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
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
                                    if (!isPublished) {
                                        showPublishWarning = true
                                    } else {
                                        isPublishing = true
                                        scope.launch {
                                            onPublish()
                                            isPublishing = false
                                        }
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
                                Text(
                                    if (hasUpdate) "Update" else "Publish",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
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
    hasUpdate: Boolean,
    localDeviceId: String,
    onInstall: suspend () -> Unit,
    onUnpublish: suspend () -> Unit
) {
    var isUnpublishing by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var localDownloadCount by remember { mutableIntStateOf(storeExt.downloadCount) }

    val scope = rememberCoroutineScope()
    val isOwner = storeExt.publisherId == localDeviceId && localDeviceId.isNotEmpty()

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

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "$localDownloadCount Download(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.weight(1f))

                if (isOwner) {
                    TextButton(
                        onClick = {
                            isUnpublishing = true
                            scope.launch {
                                onUnpublish()
                                isUnpublishing = false
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        enabled = !isUnpublishing
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

                if (isInstalled && !hasUpdate) {
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
                            if (!hasUpdate) localDownloadCount += 1

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
                        val buttonText = when {
                            isInstalling -> if (hasUpdate) "Updating..." else "Installing..."
                            hasUpdate -> "Update"
                            else -> "Install"
                        }
                        Text(buttonText)
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
