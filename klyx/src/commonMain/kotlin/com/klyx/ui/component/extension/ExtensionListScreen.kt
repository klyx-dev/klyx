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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import com.klyx.core.LocalNotifier
import com.klyx.core.app.LocalApp
import com.klyx.core.file.toKxFile
import com.klyx.core.net.isNotConnected
import com.klyx.core.net.rememberNetworkState
import com.klyx.di.LocalExtensionViewModel
import com.klyx.extension.ExtensionManifest
import com.klyx.extension.host.ExtensionStore
import com.klyx.icons.Icons
import com.klyx.icons.Search
import com.klyx.icons.SearchOff
import com.klyx.resources.Res.string
import com.klyx.resources.extension_install_dev_button
import com.klyx.resources.extension_screen_title
import com.klyx.resources.extension_search_placeholder
import com.klyx.resources.no_extensions
import com.klyx.resources.no_internet_connection
import com.klyx.ui.theme.DefaultKlyxShape
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExtensionListScreen(
    modifier: Modifier = Modifier,
    onExtensionItemClick: (ExtensionManifest) -> Unit
) {
    val notifier = LocalNotifier.current
    val viewModel = LocalExtensionViewModel.current
    val state by viewModel.extensionListState.collectAsState()
    val app = LocalApp.current
    val store = remember { ExtensionStore.global(app) }

    var installTask by remember { mutableStateOf<Deferred<Unit>?>(null) }
    val selectDir = rememberDirectoryPickerLauncher { file ->
        if (file != null) {
            installTask = app.backgroundSpawn {
                viewModel.installDevFromDirectory(file.toKxFile(), store)
            }
        }
    }

    val networkState by rememberNetworkState()

    LaunchedEffect(Unit) {
        if (networkState.isNotConnected) notifier.toast("No internet")
        viewModel.loadExtensions(store)
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
                onClick = { selectDir.launch() },
                shape = DefaultKlyxShape
            ) {
                Text(
                    text = stringResource(string.extension_install_dev_button),
                    modifier = Modifier.background(Color.Transparent)
                )
            }
        }

        var showSearchBar by remember { mutableStateOf(false) }

        AnimatedVisibility(
            visible = showSearchBar,
            enter = fadeIn() + slideInVertically() + expandVertically(),
            exit = fadeOut() + slideOutVertically() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                placeholder = { Text(stringResource(string.extension_search_placeholder)) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                leadingIcon = {
                    Icon(
                        Icons.Search,
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
                onSelect = viewModel::onFilterChanged,
                selectedFilter = state.filter,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { showSearchBar = !showSearchBar }) {
                Icon(
                    if (!showSearchBar) Icons.Search else Icons.SearchOff,
                    contentDescription = null
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        AnimatedVisibility(visible = state.isLoading) {
            Column {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        val filteredExtensions = viewModel.getFilteredExtensions(state)
        val installedIds = state.installedExtensions.fastMap { it.id }

        if (filteredExtensions.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredExtensions) { manifest ->
                    ExtensionCard(
                        manifest = manifest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        isInstalled = manifest.id in installedIds,
                        onClick = { onExtensionItemClick(manifest) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularWavyProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading extensions...")
                        }
                    }

                    networkState.isNotConnected -> {
                        Text(stringResource(string.no_internet_connection))
                    }

                    else -> {
                        Text(stringResource(string.no_extensions))
                    }
                }
            }
        }
    }

    installTask?.let {
        LaunchedEffect(Unit) {
            it.await()
            installTask = null
        }

        AlertDialog(
            onDismissRequest = {
                installTask!!.cancel("Dialog dismissed")
                notifier.toast("Installation cancelled")
                installTask = null
            },
            title = { Text("Installing...") },
            text = { Text("Please wait while the extension is being installed.") },
            confirmButton = {}
        )
    }
}
