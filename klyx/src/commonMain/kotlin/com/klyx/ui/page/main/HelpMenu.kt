package com.klyx.ui.page.main

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
import com.klyx.icons.Article
import com.klyx.icons.ChevronRight
import com.klyx.icons.Feedback
import com.klyx.icons.FormatListBulleted
import com.klyx.icons.Icons
import com.klyx.icons.Info
import com.klyx.icons.Keyboard
import com.klyx.icons.Report
import com.klyx.icons.RestartAlt
import com.klyx.icons.Settings
import com.klyx.icons.Share
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
                    Icons.Feedback,
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
                    Icons.Article,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = { Text("Keyboard Shortcuts") },
            onClick = { uriHandler.openUri(KEYBOARD_SHORTCUTS_URL) },
            leadingIcon = {
                Icon(
                    Icons.Keyboard,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = { Text("Report Issue") },
            onClick = { uriHandler.openUri(REPORT_ISSUE_URL) },
            leadingIcon = {
                Icon(
                    Icons.Report,
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
                    Icons.ChevronRight,
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
                    Icons.Settings,
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
                    Icons.FormatListBulleted,
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
                    Icons.Share,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            text = { Text("Restart App") },
            onClick = { restartApp(isKillProcess = true) },
            leadingIcon = {
                Icon(
                    Icons.RestartAlt,
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
                    Icons.Info,
                    contentDescription = null
                )
            }
        )
    }
}

internal expect fun restartApp(isKillProcess: Boolean = true)
