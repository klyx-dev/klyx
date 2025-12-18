package com.klyx.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun KlyxDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = AlertDialogDefaults.shape,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
    confirmButton: @Composable (() -> Unit) = {}
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = content,
        shape = shape
    )
}

@Composable
fun KlyxConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    KlyxDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = { Text(title) },
        content = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun KlyxErrorDialog(
    title: String = "Error",
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = "OK",
    modifier: Modifier = Modifier
) {
    KlyxConfirmationDialog(
        title = title,
        message = message,
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        confirmText = buttonText,
        dismissText = "",
        icon = Icons.Default.Error,
        modifier = modifier
    )
}
