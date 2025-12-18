package com.klyx.ui.component.extension

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Update
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
import com.klyx.core.extension.ExtensionInfo
import com.klyx.core.extension.fetchLastUpdated
import com.klyx.core.formatDateTime
import com.klyx.core.icon.GithubAlt
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.util.string
import com.klyx.di.LocalExtensionViewModel
import com.klyx.extension.ExtensionManager
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
    extensionInfo: ExtensionInfo,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val notifier = LocalNotifier.current
    val viewModel = LocalExtensionViewModel.current

    val scope = rememberCoroutineScope()

    val listState by viewModel.extensionListState.collectAsState()
    val installedIds = listState.installedExtensions.fastMap { it.id }.toSet()
    val isInstalled = extensionInfo.id in installedIds

    var isInstalling by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(vertical = 8.dp),
            text = extensionInfo.name,
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
            Text(extensionInfo.description)

            TextWithIcon(
                Icons.Outlined.Person,
                text = extensionInfo.authors.joinToString(", ")
            )

            TextWithIcon(
                Icons.Outlined.History,
                text = "Version: ${extensionInfo.version}"
            )

            val installedExt = ExtensionManager.findInstalledExtension(extensionInfo.id)
            val isDevExtension = installedExt?.isDevExtension == true

            if (!isDevExtension) {
                TextWithIcon(
                    Icons.Outlined.Download,
                    text = "Downloads: (Not available)"
                )

                val lastUpdated by produceState("") {
                    value = getLastUpdateText(extensionInfo.repository)
                }

                TextWithIcon(
                    Icons.Outlined.CalendarToday,
                    text = lastUpdated.ifEmpty { "Last updated on ..." }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                val updateAvailable = viewModel.isUpdateAvailable(extensionInfo.id)
                var isUpdating by remember { mutableStateOf(false) }

                val (icon, labelRes) = when {
                    isInstalling -> Icons.Outlined.Download to string.installing
                    isInstalled && updateAvailable && isUpdating -> Icons.Outlined.Update to string.updating
                    isInstalled && updateAvailable -> Icons.Outlined.Update to string.update
                    isInstalled -> Icons.Outlined.Delete to string.action_uninstall
                    else -> Icons.Outlined.Download to string.action_install
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
                                viewModel.uninstallExtension(extensionInfo)
                                notifier.notify(string(string.extension_uninstall_restart_prompt))
                                onNavigateBack()
                            } else {
                                scope.launch {
                                    isInstalling = true
                                    if (updateAvailable) isUpdating = true
                                    viewModel.installExtension(extensionInfo)
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

                if (extensionInfo.repository.isNotEmpty()) {
                    ElevatedButton(
                        onClick = { uriHandler.openUri(extensionInfo.repository) },
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

private suspend fun getLastUpdateText(repo: String): String {
    return try {
        val time = fetchLastUpdated(repo) ?: return "Last updated: unknown"
        val formatted = time.formatDateTime()
        "Last updated on $formatted"
    } catch (_: Throwable) {
        "Last updated: unknown"
    }
}
