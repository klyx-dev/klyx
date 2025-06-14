package com.klyx.ui.component.extension

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.documentfile.provider.DocumentFile
import com.klyx.core.compose.LocalExtensionFactory
import com.klyx.core.extension.ExtensionToml
import com.klyx.core.extension.fetchExtensions
import com.klyx.core.extension.installExtension
import com.klyx.core.file.DocumentFileWrapper
import com.klyx.core.file.wrapFile
import com.klyx.core.icons.GithubAlt
import com.klyx.core.icons.KlyxIcons
import com.klyx.core.net.isConnected
import com.klyx.core.rememberNetworkState
import com.klyx.core.showShortToast
import com.klyx.core.spacedName
import com.klyx.extension.ExtensionFilter
import com.klyx.extension.ExtensionManager
import com.klyx.ui.theme.DefaultKlyxShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExtensionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val factory = LocalExtensionFactory.current

    val scope = rememberCoroutineScope()
    val networkState by rememberNetworkState()

    val extensions = remember { mutableStateListOf<ExtensionToml>() }

    var isLoading by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf(ExtensionFilter.All) }

    LaunchedEffect(networkState) {
        if (networkState.isConnected) {
            isLoading = true
            fetchExtensions().onSuccess { extensions += it }.onFailure {
                context.showShortToast("${it.message}")
            }
            isLoading = false
        } else {
            context.showShortToast("No internet connection")
            isLoading = false
        }
    }

    val selectDir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            if (uri.host != "com.termux.documents") {
                val dir = DocumentFileWrapper(DocumentFile.fromTreeUri(context, uri)!!, isDocumentTree = true)

                scope.launch {
                    ExtensionManager.installExtension(
                        context = context,
                        dir = dir,
                        factory = factory,
                        isDevExtension = true
                    ).onFailure {
                        context.showShortToast("Failed to install extension: ${it.message}")
                    }
                }
            } else {
                context.showShortToast("Select a directory from your device's storage. External storage providers are not supported.")
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Extensions",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            ElevatedButton(
                onClick = { selectDir.launch(null) },
                shape = DefaultKlyxShape,
                colors = ButtonDefaults.elevatedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "Install Dev Extension",
                    modifier = Modifier.background(Color.Transparent)
                )
            }
        }

        var searchQuery by remember { mutableStateOf("") }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            placeholder = { Text("Search extensions...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
        )

        ExtensionFilterBar(onFilterChange = { filter = it })
        //HorizontalDivider()
        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                //CircularWavyProgressIndicator()
                LinearWavyProgressIndicator()
            }
        } else {
            val installedExtensions = ExtensionManager.installedExtensions.fastMap { it.toml }

            val filteredExtensions = when (filter) {
                ExtensionFilter.All -> installedExtensions + extensions.fastFilter { it !in installedExtensions }
                ExtensionFilter.Installed -> installedExtensions
                ExtensionFilter.NotInstalled -> extensions
            }.fastFilter { it.name.contains(searchQuery, ignoreCase = true) }

            if (filteredExtensions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredExtensions, key = { it.id }) { extension ->
                        Card(
                            shape = DefaultKlyxShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.animateItem()
                        ) {
                            var isInstalling by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = extension.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "v${extension.version}",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        text = if (extension in installedExtensions) {
                                            "Uninstall"
                                        } else "Install",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .clip(DefaultKlyxShape)
                                            .alpha(if (isInstalling) 0.5f else 1f)
                                            .clickable(role = Role.Button, enabled = !isInstalling) {
                                                if (extension in installedExtensions) {
                                                    ExtensionManager.uninstallExtension(extension)
                                                    context.showShortToast("Restart the app to complete the uninstall process.")
                                                } else {
                                                    scope.launch {
                                                        isInstalling = true
                                                        installExtension(extension).onSuccess { file ->
                                                            ExtensionManager.installExtension(
                                                                context = context,
                                                                dir = file.wrapFile(),
                                                                factory = factory,
                                                                isDevExtension = false
                                                            ).onSuccess {
                                                                context.showShortToast("Extension installed successfully")
                                                            }.onFailure {
                                                                context.showShortToast("Failed to install extension: ${it.message}")
                                                            }
                                                        }.onFailure {
                                                            context.showShortToast("Failed to install extension: ${it.message}")
                                                        }
                                                        isInstalling = false
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 4.dp),
                                        color = if (ExtensionManager.findExtension(extension.id)?.isDevExtension == true) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Author${if (extension.authors.size > 1) "s" else ""}: ${extension.authors.joinToString(",")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    if (ExtensionManager.findExtension(extension.id)?.isDevExtension == true) {
                                        Icon(
                                            Icons.Outlined.Code,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(end = 4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "Downloads: N/A",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = extension.description,
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    val isDevExtension = remember(extension) {
                                        ExtensionManager.findExtension(extension.id)?.isDevExtension == true
                                    }

                                    if (!isDevExtension) {
                                        if (extension.repository.isNotBlank()) {
                                            Icon(
                                                KlyxIcons.GithubAlt,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(DefaultKlyxShape)
                                                    .clickable(role = Role.Button) { uriHandler.openUri(extension.repository) }
                                                    .padding(4.dp)
                                            )

                                            Spacer(modifier = Modifier.width(2.dp))
                                        }

                                        Icon(
                                            Icons.Outlined.MoreHoriz,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(DefaultKlyxShape)
                                                .clickable(role = Role.Button) { context.showShortToast("Nothing...") }
                                                .padding(2.dp)
                                        )
                                    }
                                }

                                if (isInstalling) {
                                    LinearWavyProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No extensions")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExtensionFilterBar(
    onFilterChange: (ExtensionFilter) -> Unit = {}
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedIndex) {
        onFilterChange(ExtensionFilter.entries[selectedIndex])
    }

    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = ButtonGroupDefaults.HorizontalArrangement
    ) {
        ExtensionFilter.entries.fastForEachIndexed { index, filter ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = {
                    selectedIndex = index
                    //onFilterChange(filter)
                },
                modifier = Modifier
                    .semantics { role = Role.RadioButton }
                    .weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ExtensionFilter.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(filter.spacedName)
            }
        }
    }
}
