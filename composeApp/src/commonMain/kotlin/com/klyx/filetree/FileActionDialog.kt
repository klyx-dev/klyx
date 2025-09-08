package com.klyx.filetree

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
            TextButton(onClick = {
                val success = if (inputLabel != null) {
                    if (textValue.isNotBlank()) onConfirm(textValue)
                    else false
                } else {
                    onConfirm("")
                }
                if (success) onDismiss()
            }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            when {
                inputLabel != null -> OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(inputLabel) },
                    singleLine = true
                )

                message != null -> Text(message)
            }
        }
    )
}

