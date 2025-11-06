package com.klyx.ui.page.settings.general

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined._60fpsSelect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextAlign
import com.klyx.LocalNavigator
import com.klyx.core.settings.LocalAppSettings
import com.klyx.core.settings.update
import com.klyx.core.ui.component.ConfirmButton
import com.klyx.core.ui.component.DismissButton
import com.klyx.core.ui.component.PreferenceSubtitle
import com.klyx.core.ui.component.PreferenceSwitch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GeneralPreferences() {
    val appSettings = LocalAppSettings.current

    val navigator = LocalNavigator.current
    @Suppress("DEPRECATION")
    BackHandler { navigator.navigateBack() }

    var showComposeEditorWarning by remember { mutableStateOf(false) }

    LazyColumn {
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

        item {
            PreferenceSwitch(
                title = "Use Compose Editor (Unstable)",
                icon = Icons.Outlined.Code,
                description = "Switch to the experimental Compose-based editor instead of the Sora editor.",
                isChecked = appSettings.useComposeEditorInsteadOfSoraEditor,
                onClick = { enabled ->
                    if (!enabled) {
                        appSettings.update { it.copy(useComposeEditorInsteadOfSoraEditor = false) }
                    } else {
                        showComposeEditorWarning = true
                    }
                }
            )
        }
    }

    if (showComposeEditorWarning) {
        AlertDialog(
            onDismissRequest = { showComposeEditorWarning = false },
            confirmButton = {
                ConfirmButton {
                    showComposeEditorWarning = false
                    appSettings.update { it.copy(useComposeEditorInsteadOfSoraEditor = true) }
                }
            },
            dismissButton = { DismissButton { showComposeEditorWarning = false } },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null) },
            title = { Text("Experimental Feature", textAlign = TextAlign.Center) },
            text = {
                Text(
                    text = "The Compose editor is unstable and may cause unexpected behavior, crashes, or performance issues.\n\n" +
                            "Use it only for testing or experimentation, not for regular editing.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        )
    }
}
