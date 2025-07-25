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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.documentfile.provider.DocumentFile
import com.klyx.core.LocalNotifier
import com.klyx.core.Notifier
import com.klyx.core.extension.ExtensionFilter
import com.klyx.core.extension.ExtensionToml
import com.klyx.core.extension.fetchExtensions
import com.klyx.core.extension.installExtension
import com.klyx.core.file.toKxFile
import com.klyx.core.icon.GithubAlt
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.net.isConnected
import com.klyx.core.net.rememberNetworkState
import com.klyx.core.string
import com.klyx.extension.ExtensionManager
import com.klyx.res.Res.string
import com.klyx.res.action_install
import com.klyx.res.action_uninstall
import com.klyx.res.extension_author_label_plural
import com.klyx.res.extension_author_label_singular
import com.klyx.res.extension_downloads
import com.klyx.res.extension_install_dev_button
import com.klyx.res.extension_install_failed
import com.klyx.res.extension_install_success
import com.klyx.res.extension_screen_title
import com.klyx.res.extension_search_placeholder
import com.klyx.res.extension_select_directory_unsupported_provider
import com.klyx.res.extension_uninstall_restart_prompt
import com.klyx.res.no_extensions
import com.klyx.res.no_internet_connection
import com.klyx.spacedName
import com.klyx.ui.theme.DefaultKlyxShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun ExtensionScreen(modifier: Modifier) {
    val context = LocalContext.current
    val notifier = LocalNotifier.current

    val scope = rememberCoroutineScope()
    val networkState by rememberNetworkState()

    val extensions = remember { mutableStateListOf<ExtensionToml>() }

    var isLoading by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf(ExtensionFilter.All) }
    var showDevExtensionInstallationType by remember { mutableStateOf(false) }

    LaunchedEffect(networkState) {
        if (networkState.isConnected) {
            isLoading = true
            fetchExtensions().onSuccess { extensions += it }.onFailure {
                notifier.error(it.message ?: it.stackTrace.first().toString())
            }
            isLoading = false
        } else {
            notifier.warning(string(string.no_internet_connection))
            isLoading = false
        }
    }

    val selectDir =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                if (uri.host != "com.termux.documents") {
                    val dir = DocumentFile.fromTreeUri(context, uri)!!.toKxFile()

                    scope.launch {
                        ExtensionManager.installExtension(
                            dir = dir,
                            isDevExtension = true
                        ).onFailure {
                            notifier.error(
                                string(
                                    string.extension_install_failed,
                                    it.message.toString()
                                )
                            )
                        }.onSuccess {
                            notifier.success(string(string.extension_install_success))
                        }
                    }
                } else {
                    notifier.notify(string(string.extension_select_directory_unsupported_provider))
                }
            }
        }

    val selectZip =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    ExtensionManager.installExtensionFromZip(
                        zipFile = DocumentFile.fromSingleUri(context, uri)!!.toKxFile(),
                        isDevExtension = true
                    ).onFailure {
                        it.printStackTrace()
                        notifier.error(
                            string(
                                string.extension_install_failed,
                                it.message
                            )
                        )
                    }
                }
            }
        }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(string.extension_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            ElevatedButton(
                onClick = { showDevExtensionInstallationType = true },
                shape = DefaultKlyxShape,
                colors = ButtonDefaults.elevatedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = stringResource(string.extension_install_dev_button),
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
            placeholder = { Text(stringResource(string.extension_search_placeholder)) },
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
                ExtensionFilter.NotInstalled -> extensions.fastFilter { it !in installedExtensions }
            }.fastFilter { it.name.contains(searchQuery, ignoreCase = true) }

            if (filteredExtensions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredExtensions) { extension ->
                        Card(
                            shape = DefaultKlyxShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.animateItem()
                        ) {
                            ExtensionItem(
                                extension = extension,
                                searchQuery = searchQuery,
                                installedExtensions = installedExtensions,
                                scope = scope
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(string.no_extensions))
                }
            }
        }
    }

    if (showDevExtensionInstallationType) {
        SelectInstallationType(
            onDismissRequest = { showDevExtensionInstallationType = false },
            onTypeSelected = { type ->
                showDevExtensionInstallationType = false
                when (type) {
                    InstallationType.Directory -> selectDir.launch(null)
                    InstallationType.Zip -> selectZip.launch(arrayOf("application/zip"))
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ExtensionItem(
    extension: ExtensionToml,
    searchQuery: String,
    installedExtensions: List<ExtensionToml>,
    scope: CoroutineScope
) {
    val colorScheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val notifier = LocalNotifier.current

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
                text = buildAnnotatedString {
                    val name = extension.name
                    val query = searchQuery
                    val startIndex = if (query.isNotBlank()) {
                        name.indexOf(query, ignoreCase = true)
                    } else -1

                    if (startIndex >= 0 && query.isNotBlank()) {
                        append(name.substring(0, startIndex))
                        withStyle(SpanStyle(color = colorScheme.primary)) {
                            append(name.substring(startIndex, startIndex + query.length))
                        }
                        append(name.substring(startIndex + query.length))
                    } else {
                        append(name)
                    }
                },
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
                    stringResource(string.action_uninstall)
                } else stringResource(string.action_install),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(DefaultKlyxShape)
                    .alpha(if (isInstalling) 0.5f else 1f)
                    .clickable(
                        role = Role.Button,
                        enabled = !isInstalling
                    ) {
                        if (extension in installedExtensions) {
                            ExtensionManager.uninstallExtension(extension)
                            notifier.notify(string(string.extension_uninstall_restart_prompt))
                        } else {
                            scope.launch {
                                isInstalling = true
                                install(extension, notifier)
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
                text = string(
                    if (extension.authors.size > 1) {
                        string.extension_author_label_plural
                    } else {
                        string.extension_author_label_singular
                    },
                    extension.authors.joinToString(",")
                ),
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
                    text = string(
                        string.extension_downloads,
                        "N/A"
                    ),
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
                            .clickable(
                                role = Role.Button
                            ) {
                                uriHandler.openUri(extension.repository)
                            }
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
                        .clickable(role = Role.Button) { notifier.notify("Nothing...") }
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

private suspend fun install(
    extension: ExtensionToml,
    notifier: Notifier
) {
    installExtension(extension).onSuccess { file ->
        ExtensionManager.installExtension(
            dir = file,
            isDevExtension = false
        ).fold(
            onFailure = {
                notifier.error(
                    string(
                        string.extension_install_failed,
                        it.message ?: it.stackTrace.first().toString()
                    )
                )
            },
            onSuccess = {
                notifier.success(string(string.extension_install_success))
            }
        )
    }.onFailure {
        notifier.error(
            string(
                string.extension_install_failed,
                it.message ?: it.stackTrace.first().toString()
            )
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal actual fun ExtensionFilterBar(onFilterChange: (ExtensionFilter) -> Unit) {
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

private enum class InstallationType {
    Directory, Zip
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectInstallationType(
    onDismissRequest: () -> Unit,
    onTypeSelected: (InstallationType) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            TextButton(
                onClick = { onTypeSelected(InstallationType.Directory) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "From Directory")
            }

            TextButton(
                onClick = { onTypeSelected(InstallationType.Zip) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "From Zip")
            }
        }
    }
}
