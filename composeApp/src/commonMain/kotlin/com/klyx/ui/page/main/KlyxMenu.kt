package com.klyx.ui.page.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.klyx.core.Environment
import com.klyx.core.LocalBuildVariant
import com.klyx.core.LocalPlatformContext
import com.klyx.core.file.KxFile
import com.klyx.core.file.shareFile
import com.klyx.core.isDebug
import com.klyx.core.ui.component.DropdownMenuDivider
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.openLogViewer
import com.klyx.viewmodel.openSettings

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KlyxMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    editorViewModel: EditorViewModel,
    klyxViewModel: KlyxViewModel
) {
    val buildVariant = LocalBuildVariant.current

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
            text = { Text("Logs") },
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

        if (buildVariant.isDebug) {
            val context = LocalPlatformContext.current
            val file1 = KxFile(Environment.DeviceHomeDir, "klyx/app_logs.txt")
            val file2 = KxFile(Environment.HomeDir, "app_logs.txt")

            DropdownMenuItem(
                text = { Text("Share Logs File") },
                onClick = {
                    context.shareFile(if (file1.exists) file1 else file2)
                    onDismissRequest()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = null
                    )
                }
            )
        }

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
            text = { Text("Info") },
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
