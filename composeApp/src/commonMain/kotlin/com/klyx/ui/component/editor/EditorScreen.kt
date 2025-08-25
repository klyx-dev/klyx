package com.klyx.ui.component.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun EditorScreen(modifier: Modifier = Modifier)

@Composable
fun PermissionDialog(
    onDismissRequest: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text("Storage Permission Required")
        },
        text = {
            Text("Klyx needs access to your device's storage to function properly. Please grant permission to continue.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRequestPermission()
                    onDismissRequest()
                }
            ) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
