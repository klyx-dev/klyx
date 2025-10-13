package com.klyx.ui.page.settings.general

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined._60fpsSelect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.klyx.core.settings.LocalAppSettings
import com.klyx.core.settings.update
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.PreferenceSubtitle
import com.klyx.core.ui.component.PreferenceSwitch
import com.klyx.res.Res
import com.klyx.res.general_settings
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralPreferences(onNavigateBack: () -> Unit) {
    val appSettings = LocalAppSettings.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.general_settings)) },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item { PreferenceSubtitle("Application") }

            item {
                PreferenceSwitch(
                    title = "Show FPS",
                    icon = Icons.Outlined._60fpsSelect,
                    description = "Show the FPS counter in the UI",
                    isChecked = appSettings.showFps,
                    onClick = { showFps ->
                        appSettings.update { it.copy(showFps = showFps) }
                    }
                )
            }

            item {
                PreferenceSwitch(
                    title = "Load Extensions on Startup",
                    icon = Icons.Outlined.Extension,
                    description = "If disabled, extensions will be loaded at runtime instead of on the splash screen.",
                    isChecked = appSettings.loadExtensionsOnStartup,
                    onClick = { appSettings.update { settings -> settings.copy(loadExtensionsOnStartup = it) } }
                )
            }
        }
    }
}
