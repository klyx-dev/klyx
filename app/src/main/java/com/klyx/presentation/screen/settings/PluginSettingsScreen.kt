package com.klyx.presentation.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klyx.api.InternalKlyxApi
import com.klyx.api.plugin.PluginSettingsRegistry
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.plugin.PluginManager
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.PluginSettingsPayload

/**
 * Dedicated settings screen for a plugin.
 *
 * Only shows content if the plugin has registered a settings screen via
 * [PluginSettingsRegistry]. The plugin's registered composable is invoked with its
 * typed [com.klyx.api.plugin.PluginSettings] as the receiver.
 */
@OptIn(UnsafeGlobalAccess::class, InternalKlyxApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PluginSettingsScreen(payload: PluginSettingsPayload) {
    val navigator = LocalNavigator.current
    val settingsRegistry: PluginSettingsRegistry = GlobalApp.global()
    val pluginManager = remember { GlobalApp.global<PluginManager>() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val pluginSettings = remember(payload.id) { pluginManager.pluginSettings(payload.id) }
    val content = settingsRegistry.contentFor(payload.id)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = payload.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateBack() },
                        shape = CircleShape,
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
                }
            )
        }
    ) { innerPadding ->
        if (pluginSettings != null && content != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                content(pluginSettings)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This plugin doesn't expose any settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
