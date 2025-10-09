package com.klyx.ui.page.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.klyx.core.ui.component.DropdownMenuDivider
import com.klyx.menu.restartApp
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.openLogViewer
import com.klyx.viewmodel.openSettings

@Composable
fun KlyxMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    editorViewModel: EditorViewModel,
    klyxViewModel: KlyxViewModel
) {
    DropdownMenu(
        expanded = expanded,
        offset = DpOffset((-5).dp, 0.dp),
        shape = MaterialTheme.shapes.medium,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("Open Settings File") },
            onClick = { editorViewModel.openSettings() },
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
            onClick = { editorViewModel.openLogViewer() },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Outlined.FormatListBulleted,
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
            text = { Text("Info") },
            onClick = { klyxViewModel.showAboutDialog() },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null
                )
            }
        )
    }
}

