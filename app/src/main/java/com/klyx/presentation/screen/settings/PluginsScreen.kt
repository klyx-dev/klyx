package com.klyx.presentation.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.klyx.api.plugin.PluginDescriptor
import com.klyx.api.plugin.PluginInfo
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.ui.showFailureToast
import com.klyx.api.ui.theme.GoogleSansRounded
import com.klyx.api.util.openUrl
import com.klyx.event.UiEvent
import com.klyx.plugin.PluginViewModel
import com.klyx.presentation.components.ExpressiveMenuItem
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.SettingsScreen
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

    val scope = rememberCoroutineScope()

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

    storeUiState.installState?.let {
        PluginInstallingDialog(
            plugin = it.plugin,
            progressText = it.message ?: "",
            progress = it.progress
        )
    }

    pluginUiState.loadingState?.let {
        PluginLoadingDialog(
            descriptor = it.desc,
            step = it.message
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Plugins",
                        //fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateBack() },
                        shapes = IconButtonDefaults.shapes(
                            shape = CircleShape
                        ),
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
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val installedIds = pluginUiState.plugins.map { it.descriptor.id }.toSet()
            val installed = pluginUiState.plugins.map(CombinedItem::Installed)
            val store = storeUiState.storePlugins.filterNot { it.id in installedIds }.map(CombinedItem::Store)

            val all = installed + store

            if (all.isEmpty() && !storeUiState.storeLoading) {
                item(key = "empty") {
                    Spacer(modifier = Modifier.height(64.dp))
                    Text(
                        text = "No plugins available.\nInstall a local bundle via the option menu or publish your plugin to the registry.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (all.isNotEmpty()) {
                items(all, key = { it.key }) { item ->
                    when (item) {
                        is CombinedItem.Installed -> InstalledPluginCard(
                            plugin = item.info,
                            storeVersion = storeUiState.storePlugins.find { it.id == item.info.descriptor.id }?.version,
                            onUnload = { viewModel.unloadPlugin(item.info.descriptor.id) },
                            onDetail = { navigator.navigateTo(SettingsScreen.PluginDetail(item.info.descriptor.id)) }
                        )

                        is CombinedItem.Store -> {
                            var installing by remember { mutableStateOf(false) }

                            StorePluginCard(
                                plugin = item.plugin,
                                installing = installing,
                                onInstall = {
                                    installing = true
                                    storeViewModel.installPlugin(item.plugin) {
                                        viewModel.refresh()
                                        installing = false
                                    }
                                },
                                onDetail = { navigator.navigateTo(SettingsScreen.PluginDetail(item.plugin.id)) }
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
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                }
            }
        }
    }
}

private sealed interface CombinedItem {
    val key: String

    data class Installed(val info: PluginInfo) : CombinedItem {
        override val key get() = "installed:${info.descriptor.id}"
    }

    data class Store(val plugin: StorePlugin) : CombinedItem {
        override val key get() = "store:${plugin.id}"
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InstalledPluginCard(
    plugin: PluginInfo,
    storeVersion: String?,
    onUnload: () -> Unit,
    onDetail: () -> Unit,
) {
    val desc = plugin.descriptor
    val updateAvailable = storeVersion != null && storeVersion != desc.version

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onDetail() },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PluginIcon(plugin)

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = desc.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${desc.id}  v${desc.version}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (updateAvailable) {
                    FilledIconButton(
                        onClick = onUnload,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Rounded.Update, contentDescription = "Update available")
                    }
                } else {
                    FilledIconButton(
                        onClick = onUnload,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Uninstall plugin")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Installed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                if (updateAvailable) {
                    Text(
                        text = "v$storeVersion available",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (desc.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = desc.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            desc.author?.let { author ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "by ${author.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    author.github?.let { github ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = github,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { openUrl(github) }
                        )
                    }
                    author.url?.let { url ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Website",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { openUrl(url) }
                        )
                    }
                }
            }

            desc.links?.let { links ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    links.source?.let { LinkChip(label = "Source", url = it) }
                    links.issues?.let { LinkChip(label = "Issues", url = it) }
                    links.website?.let { LinkChip(label = "Website", url = it) }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Supports app ${desc.minAppVersion}${desc.maxAppVersion?.let { " – $it" } ?: "+"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StorePluginCard(
    plugin: StorePlugin,
    installing: Boolean,
    onInstall: () -> Unit,
    onDetail: () -> Unit,
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onDetail() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(plugin.iconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${plugin.name} icon",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    placeholder = rememberVectorPainter(Icons.Rounded.Extension),
                    error = rememberVectorPainter(Icons.Rounded.Extension),
                    contentScale = ContentScale.Fit,
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "by ${plugin.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onInstall,
                    enabled = !installing,
                    modifier = Modifier.heightIn(min = ButtonDefaults.MinHeight),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    if (installing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Install", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "v${plugin.version}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                if (plugin.downloadCount > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${plugin.downloadCount} downloads",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginIcon(plugin: PluginInfo) {
    val icon = plugin.icon
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                painter = icon,
                contentDescription = "${plugin.descriptor.name} icon",
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Extension,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun LinkChip(label: String, url: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = { openUrl(url) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Rounded.Link,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PluginInstallingDialog(
    plugin: StorePlugin,
    progressText: String,
    progress: Float? = null
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
                    progress?.let {
                        if (progress == 0f) {
                            LoadingIndicator()
                        } else {
                            LoadingIndicator(progress = { progress })
                        }
                    } ?: LoadingIndicator()
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Installing ${plugin.name}...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee(),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

//                LinearWavyProgressIndicator(
//                    progress = { progress },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(6.dp)
//                        .clip(CircleShape),
//                    trackColor = MaterialTheme.colorScheme.surfaceVariant
//                )

                if (progressText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        //modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PluginLoadingDialog(descriptor: PluginDescriptor?, step: String?) {
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
                    LoadingIndicator()
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = buildString {
                        append("Loading ")

                        if (descriptor != null) {
                            append(descriptor.name)
                        } else {
                            append("Plugin")
                        }

                        append("...")
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                step?.let {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.basicMarquee(),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
