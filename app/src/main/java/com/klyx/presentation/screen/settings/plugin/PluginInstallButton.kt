package com.klyx.presentation.screen.settings.plugin

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.klyx.app.icons.Download
import com.klyx.i18n.strings
import com.klyx.plugin.PluginManager
import com.klyx.plugin.PluginUiState
import com.klyx.presentation.components.InstallationLogCard
import com.klyx.presentation.navigation.PluginDetailPayload
import com.klyx.presentation.viewmodel.PluginStoreUiState
import com.klyx.presentation.viewmodel.StorePlugin

private const val CDN = PluginManager.CDN
private const val API = PluginManager.API

@Composable
fun PluginInstallButton(
    payload: PluginDetailPayload,
    isPluginActuallyInstalled: Boolean,
    pluginUiState: PluginUiState,
    storeUiState: PluginStoreUiState,
    onUninstall: () -> Unit,
    onBundleSourceExists: (Uri) -> Boolean,
    onInstallFromBundle: (Uri) -> Unit,
    onInstallFromStore: (StorePlugin) -> Unit,
    onNavigateBack: () -> Unit,
    reinstalling: Boolean,
    onReinstallingChange: (Boolean) -> Unit
) {
    if (isPluginActuallyInstalled) {
        val uninstalling = pluginUiState.isUnloading
        Button(
            onClick = onUninstall,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            if (uninstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.uninstalling, style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.uninstall, style = MaterialTheme.typography.labelLarge)
            }
        }
    } else {
        val installState = storeUiState.installState
        val isThisInstalling = installState?.plugin?.id == payload.id
        val installing by remember { derivedStateOf { storeUiState.installState != null } }

        Column {
            Button(
                onClick = {
                    val localSource = payload.sourceUri?.takeIf { it.isNotBlank() }
                    if (localSource != null) {
                        val uri = localSource.toUri()
                        if (onBundleSourceExists(uri)) {
                            onReinstallingChange(true)
                            onInstallFromBundle(uri)
                        } else {
                            onNavigateBack()
                        }
                    } else {
                        onInstallFromStore(
                            StorePlugin(
                                id = payload.id,
                                name = payload.name,
                                description = payload.description,
                                author = payload.author,
                                version = payload.version,
                                minAppVersion = "",
                                maxAppVersion = null,
                                downloadCount = payload.downloadCount,
                                iconUrl = payload.iconUrl ?: "$CDN/${payload.id}/icon.png",
                                downloadUrl = "$API/dl/${payload.id}/${payload.version}"
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = (!installing && !reinstalling) || isThisInstalling || reinstalling
            ) {
                when {
                    isThisInstalling -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = installState.message ?: strings.installing,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    reinstalling -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.installing,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    else -> {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (installing) strings.anotherTaskRunning else strings.install,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isThisInstalling,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (installState != null) {
                        if (installState.message != null) {
                            Text(
                                text = installState.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        if (installState.progress > 0f) {
                            LinearProgressIndicator(
                                progress = { installState.progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }
                    }
                    if (installState != null) {
                        InstallationLogCard(
                            title = strings.installationLogs,
                            logs = installState.logs
                        )
                    }
                }
            }
        }
    }
}
