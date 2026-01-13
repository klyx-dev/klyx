package com.klyx.ui.component.extension

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import com.klyx.core.LocalNotifier
import com.klyx.core.app.LocalApp
import com.klyx.core.extension.fetchLastUpdated
import com.klyx.core.formatDateTime
import com.klyx.core.icon.GithubAlt
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.util.string
import com.klyx.di.LocalExtensionViewModel
import com.klyx.extension.ExtensionManifest
import com.klyx.extension.host.ExtensionStore
import com.klyx.icons.CalendarMonth
import com.klyx.icons.Delete
import com.klyx.icons.Download
import com.klyx.icons.History
import com.klyx.icons.Icons
import com.klyx.icons.Person
import com.klyx.icons.Update
import com.klyx.resources.Res.string
import com.klyx.resources.action_install
import com.klyx.resources.action_uninstall
import com.klyx.resources.extension_uninstall_restart_prompt
import com.klyx.resources.installing
import com.klyx.resources.update
import com.klyx.resources.updating
import com.klyx.ui.theme.DefaultKlyxShape
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExtensionDetailScreen(
    manifest: ExtensionManifest,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val notifier = LocalNotifier.current
    val viewModel = LocalExtensionViewModel.current
    val app = LocalApp.current
    val store = remember { ExtensionStore.global(app) }

    val scope = rememberCoroutineScope()

    val listState by viewModel.extensionListState.collectAsState()
    val installedIds = listState.installedExtensions.fastMap { it.id }.toSet()
    val isInstalled by remember {
        derivedStateOf {
            manifest.id in installedIds
        }
    }

    var isInstalling by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(vertical = 8.dp),
            text = manifest.name,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineMedium
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 11.dp),
            thickness = Dp.Hairline
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Details",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(manifest.description.orEmpty())

            TextWithIcon(
                Icons.Person,
                text = manifest.authors.joinToString(", ")
            )

            TextWithIcon(
                Icons.History,
                text = "Version: ${manifest.version}"
            )

            val isDevExtension = store.isDevExtension(manifest.id)

            if (!isDevExtension) {
                TextWithIcon(
                    Icons.Download,
                    text = "Downloads: (Not available)"
                )

                val lastUpdated by produceState("") {
                    value = getLastUpdateText(manifest.repository)
                }

                TextWithIcon(
                    Icons.CalendarMonth,
                    text = lastUpdated.ifEmpty { "Last updated on ..." }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                val updateAvailable = viewModel.isUpdateAvailable(manifest.id)
                var isUpdating by remember { mutableStateOf(false) }

                val (icon, labelRes) = when {
                    isInstalling -> Icons.Download to string.installing
                    isInstalled && updateAvailable && isUpdating -> Icons.Update to string.updating
                    isInstalled && updateAvailable -> Icons.Update to string.update
                    isInstalled -> Icons.Delete to string.action_uninstall
                    else -> Icons.Download to string.action_install
                }

                val isUninstall = isInstalled && !updateAvailable
                val buttonColors = if (isUninstall) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else ButtonDefaults.buttonColors()

                if ((isDevExtension && isInstalled) || !isDevExtension) {
                    Button(
                        onClick = {
                            if (isUninstall) {
                                scope.launch {
                                    viewModel.uninstallExtension(manifest.id, store)
                                    notifier.notify(string(string.extension_uninstall_restart_prompt))
                                    onNavigateBack()
                                }
                            } else {
                                scope.launch {
                                    isInstalling = true
                                    if (updateAvailable) isUpdating = true
                                    viewModel.installExtension(manifest, store)
                                }.invokeOnCompletion {
                                    isInstalling = false
                                    isUpdating = false
                                }
                            }
                        },
                        shape = DefaultKlyxShape,
                        colors = buttonColors,
                        enabled = !isInstalling,
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(icon, modifier = Modifier.size(14.dp), contentDescription = null)
                        }

                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(labelRes))
                    }

                    Spacer(Modifier.width(8.dp))
                }

                if (!manifest.repository.isNullOrEmpty()) {
                    ElevatedButton(
                        onClick = { uriHandler.openUri(manifest.repository!!) },
                        shape = DefaultKlyxShape
                    ) {
                        Icon(
                            KlyxIcons.GithubAlt,
                            modifier = Modifier.size(14.dp),
                            contentDescription = null,
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text("Visit Repository")
                    }
                }
            }
        }
    }
}

@Composable
private fun TextWithIcon(
    imageVector: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = color
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private suspend fun getLastUpdateText(repo: String?): String {
    return try {
        val time = repo?.let { fetchLastUpdated(it) } ?: return "Last updated: unknown"
        val formatted = time.formatDateTime()
        "Last updated on $formatted"
    } catch (_: Throwable) {
        "Last updated: unknown"
    }
}
