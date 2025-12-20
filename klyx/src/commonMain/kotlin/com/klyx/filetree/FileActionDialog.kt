package com.klyx.filetree

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.klyx.core.ui.component.ConfirmButton
import com.klyx.core.ui.component.DismissButton

@Composable
fun FileActionDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    title: String,
    confirmLabel: String,
    onConfirm: (String) -> Boolean,
    inputLabel: String? = null,
    initialValue: String = "",
    message: String? = null,
) {
    if (!show) return

    var textValue by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            ConfirmButton(text = confirmLabel) {
                val success = if (inputLabel != null) {
                    if (textValue.isNotBlank()) onConfirm(textValue)
                    else false
                } else {
                    onConfirm("")
                }
                if (success) onDismiss()
            }
        },
        dismissButton = { DismissButton(onClick = onDismiss) },
        title = { Text(title) },
        text = {
            when {
                inputLabel != null -> OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(inputLabel) },
                    singleLine = true
                )

                message != null -> Text(message, style = MaterialTheme.typography.bodyLarge)
            }
        }
    )
}

