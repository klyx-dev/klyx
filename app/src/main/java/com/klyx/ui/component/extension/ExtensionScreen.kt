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
import androidx.compose.material3.CircularWavyProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.documentfile.provider.DocumentFile
import com.klyx.core.compose.LocalExtensionFactory
import com.klyx.core.file.DocumentFileWrapper
import com.klyx.core.icons.GithubAlt
import com.klyx.core.icons.KlyxIcons
import com.klyx.core.showShortToast
import com.klyx.core.spacedName
import com.klyx.extension.ExtensionFilter
import com.klyx.extension.ExtensionManager
import com.klyx.ui.theme.DefaultKlyxShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExtensionScreen(modifier: Modifier = Modifier) {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1000)
        isLoading = false
    }

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val factory = LocalExtensionFactory.current
    val scope = rememberCoroutineScope()

    val selectDir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            if (uri.host != "com.termux.documents") {
                val dir = DocumentFileWrapper(DocumentFile.fromTreeUri(context, uri)!!, isDocumentTree = true)

                scope.launch {
                    ExtensionManager.installExtension(
                        context = context,
                        dir = dir,
                        factory = factory,
                        isDevExtension = true,
                        onError = { _, exception ->
                            context.showShortToast("Error installing extension: ${exception.message}")
                        }
                    )
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

        ExtensionFilterBar()
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
            val extensions = ExtensionManager.installedExtensions.filter {
                it.toml.name.contains(searchQuery, ignoreCase = true)
            }

            if (extensions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(extensions) { extension ->
                        Card(
                            shape = DefaultKlyxShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .fillMaxWidth()
                            ) {
                                val toml = extension.toml

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = toml.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "v${toml.version}",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        text = "Uninstall",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .clip(DefaultKlyxShape)
                                            .clickable(role = Role.Button) {
                                                ExtensionManager.uninstallExtension(extension)
                                            }
                                            .padding(horizontal = 4.dp),
                                        color = if (extension.isDevExtension) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Author${if (toml.authors.size > 1) "s" else ""}: ${toml.authors.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    if (extension.isDevExtension) {
                                        Icon(
                                            Icons.Outlined.Code,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(end = 4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "Downloads: 0",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = toml.description,
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    if (extension.isDevExtension) {
                                        if (toml.repository.isNotBlank()) {
                                            Icon(
                                                KlyxIcons.GithubAlt,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(DefaultKlyxShape)
                                                    .clickable(role = Role.Button) { uriHandler.openUri(toml.repository) }
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
                                                .clickable(role = Role.Button) {
                                                    context.showShortToast("Nothing...")
                                                }
                                                .padding(2.dp)
                                        )
                                    }
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
