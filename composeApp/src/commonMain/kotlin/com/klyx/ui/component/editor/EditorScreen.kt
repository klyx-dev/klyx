package com.klyx.ui.component.editor

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.klyx.ui.component.KlyxDialog

@Composable
expect fun EditorScreen(modifier: Modifier = Modifier)

@Composable
fun PermissionDialog(
    onDismissRequest: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    KlyxDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = "Storage Permission Required",
        message = "To open this file, Klyx needs access to your device's storage. Please grant permission to continue.",
        positiveButton = {
            TextButton(
                onClick = {
                    onRequestPermission()
                    onDismissRequest()
                }
            ) {
                Text("Grant Permission")
            }
        },
        negativeButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
