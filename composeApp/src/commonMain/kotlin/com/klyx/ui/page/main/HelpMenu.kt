package com.klyx.ui.page.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.klyx.core.DOCS_URL
import com.klyx.core.KEYBOARD_SHORTCUTS_URL
import com.klyx.core.REPORT_ISSUE_URL
import com.klyx.core.ui.component.DropdownMenuDivider
import com.klyx.viewmodel.KlyxViewModel

@Composable
fun HelpMenu(
    expanded: Boolean,
    klyxViewModel: KlyxViewModel,
    onDismissRequest: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

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
    }
}
