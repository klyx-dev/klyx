package com.klyx.ui.page.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.klyx.core.DOCS_URL
import com.klyx.core.KEYBOARD_SHORTCUTS_URL
import com.klyx.core.LocalPlatformContext
import com.klyx.core.REPORT_ISSUE_URL
import com.klyx.core.file.shareFile
import com.klyx.core.file.toKxFile
import com.klyx.core.icon.Discord
import com.klyx.core.icon.Klyx
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.io.Paths
import com.klyx.core.io.logFile
import com.klyx.core.ui.component.DropdownMenuDivider
import com.klyx.di.LocalEditorViewModel
import com.klyx.di.LocalKlyxViewModel
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.openLogViewer
import com.klyx.viewmodel.openSettings

@Composable
fun HelpMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    val editorViewModel = LocalEditorViewModel.current
    val klyxViewModel = LocalKlyxViewModel.current

    val uriHandler = LocalUriHandler.current

    var showKlyxMenu by rememberSaveable { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        offset = DpOffset((-5).dp, 0.dp),
        shape = MaterialTheme.shapes.medium,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("Give Feedback...") },
            onClick = {
                klyxViewModel.showGiveFeedbackDialog()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Feedback,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = { Text("Discord Community") },
            onClick = { uriHandler.openUri("https://discord.gg/ZEUHXymRVy") },
            leadingIcon = {
                Icon(
                    KlyxIcons.Discord,
                    contentDescription = null
                )
            }
        )

        DropdownMenuDivider()

        DropdownMenuItem(
            text = { Text("Documentation") },
            onClick = { uriHandler.openUri(DOCS_URL) },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Outlined.Article,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = { Text("Keyboard Shortcuts") },
            onClick = { uriHandler.openUri(KEYBOARD_SHORTCUTS_URL) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Keyboard,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = { Text("Report Issue") },
            onClick = { uriHandler.openUri(REPORT_ISSUE_URL) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Report,
                    contentDescription = null
                )
            }
        )

        DropdownMenuDivider()

        DropdownMenuItem(
            text = { Text("Klyx") },
            onClick = {
                onDismissRequest()
                showKlyxMenu = !showKlyxMenu
            },
            leadingIcon = {
                Icon(
                    KlyxIcons.Klyx,
                    contentDescription = "Klyx Menu"
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        )
    }

    KlyxMenu(
        expanded = showKlyxMenu,
        editorViewModel = editorViewModel,
        klyxViewModel = klyxViewModel,
        onDismissRequest = { showKlyxMenu = false }
    )
}

@Composable
private fun KlyxMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    klyxViewModel: KlyxViewModel,
    editorViewModel: EditorViewModel
) {
    val context = LocalPlatformContext.current

    DropdownMenu(
        expanded = expanded,
        offset = DpOffset((-5).dp, 0.dp),
        shape = MaterialTheme.shapes.medium,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("Open Settings File") },
            onClick = {
                editorViewModel.openSettings()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null
                )
            }
        )

        DropdownMenuDivider()

        DropdownMenuItem(
            text = { Text("App Logs") },
            onClick = {
                editorViewModel.openLogViewer()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Outlined.FormatListBulleted,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = { Text("Share Logs File") },
            onClick = {
                context.shareFile(Paths.logFile.toKxFile())
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = { Text("Restart App") },
            onClick = { restartApp(isKillProcess = true) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.RestartAlt,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = { Text("App Info") },
            onClick = {
                klyxViewModel.showInfoDialog()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null
                )
            }
        )
    }
}

internal expect fun restartApp(isKillProcess: Boolean = true)
