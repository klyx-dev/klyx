@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.klyx.ui.component.extension

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.klyx.core.LocalNotifier
import com.klyx.core.event.ObserverAsEvents
import com.klyx.core.extension.ExtensionFilter
import com.klyx.core.extension.ExtensionInfo
import com.klyx.core.file.toKxFile
import com.klyx.core.net.isNotConnected
import com.klyx.core.net.rememberNetworkState
import com.klyx.core.string
import com.klyx.extension.ExtensionManager
import com.klyx.res.Res.string
import com.klyx.res.extension_install_dev_button
import com.klyx.res.extension_install_failed
import com.klyx.res.extension_install_success
import com.klyx.res.extension_screen_title
import com.klyx.res.extension_search_placeholder
import com.klyx.res.no_extensions
import com.klyx.res.no_internet_connection
import com.klyx.ui.theme.DefaultKlyxShape
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExtensionListScreen(
    onExtensionItemClick: (ExtensionInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifier = LocalNotifier.current
    val coroutineScope = rememberCoroutineScope()
    var showDevExtensionInstallSheet by remember { mutableStateOf(false) }
    val viewModel = koinViewModel<ExtensionViewModel>()
    val state by viewModel.extensionListState.collectAsState()

    val selectDir = rememberDirectoryPickerLauncher { file ->
        if (file != null) {
            coroutineScope.launch {
                ExtensionManager.installExtension(
                    directory = file.toKxFile(),
                    isDevExtension = true
                ).onFailure {
                    notifier.error(string(string.extension_install_failed, it))
                }.onSuccess {
                    notifier.success(string(string.extension_install_success))
                }
            }
        }
    }

    val selectZip = rememberFilePickerLauncher(
        type = FileKitType.File("zip")
    ) { file ->
        if (file != null) {
            coroutineScope.launch {
                ExtensionManager.installExtensionFromZip(
                    zipFile = file.toKxFile(),
                    isDevExtension = true
                ).onFailure {
                    notifier.error(string(string.extension_install_failed, it))
                }.onSuccess {
                    notifier.success(string(string.extension_install_success))
                }
            }
        }
    }

    val networkState by rememberNetworkState()
    var filter by remember { mutableStateOf(ExtensionFilter.All) }

    ObserverAsEvents(viewModel.extensionEvents) { event ->
        when (event) {
            ExtensionEvent.ReloadExtensions -> viewModel.loadExtensions()
        }
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = stringResource(string.extension_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { showDevExtensionInstallSheet = true },
                shape = DefaultKlyxShape
            ) {
                Text(
                    text = stringResource(string.extension_install_dev_button),
                    modifier = Modifier.background(Color.Transparent)
                )
            }
        }

        var searchQuery by remember { mutableStateOf("") }
        var showSearchBar by remember { mutableStateOf(false) }

        AnimatedVisibility(
            visible = showSearchBar,
            enter = fadeIn() + slideInVertically() + expandVertically(),
            exit = fadeOut() + slideOutVertically() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                placeholder = { Text(stringResource(string.extension_search_placeholder)) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null
                    )
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExtensionFilterButtons(
                onSelect = { filter = it },
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { showSearchBar = !showSearchBar }) {
                Icon(
                    if (!showSearchBar) Icons.Default.Search else Icons.Default.SearchOff,
                    contentDescription = null
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        AnimatedVisibility(state.isLoading) {
            Column {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        val installedExtensions = ExtensionManager.installedExtensions.fastMap { it.info }

        val filteredExtensions = when (filter) {
            ExtensionFilter.All -> installedExtensions + state.extensionInfos.fastFilter { it !in installedExtensions }
            ExtensionFilter.Installed -> installedExtensions
            ExtensionFilter.NotInstalled -> state.extensionInfos.fastFilter { it !in installedExtensions }
        }.fastMap { ext ->
            ext to FuzzySearch.partialRatio(searchQuery.lowercase(), ext.name.lowercase())
        }.fastFilter { (_, score) -> searchQuery.isBlank() || score >= 60 }
            .sortedByDescending { it.second }
            .fastMap { it.first }

        if (filteredExtensions.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredExtensions) { extension ->
                    ExtensionCard(
                        extension = extension,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        isInstalled = extension in installedExtensions,
                        onClick = { onExtensionItemClick(extension) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularWavyProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading extensions...")
                    }
                } else if (networkState.isNotConnected && filteredExtensions.isNotEmpty()) {
                    Text(stringResource(string.no_internet_connection))
                } else {
                    Text(stringResource(string.no_extensions))
                }
            }
        }
    }

    if (showDevExtensionInstallSheet) {
        InstallSheet(
            onDismiss = { showDevExtensionInstallSheet = false },
            onPick = { type ->
                showDevExtensionInstallSheet = false

                when (type) {
                    InstallationType.Directory -> selectDir.launch()
                    InstallationType.Zip -> selectZip.launch()
                }
            }
        )
    }
}
