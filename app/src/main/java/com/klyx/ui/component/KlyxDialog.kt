package com.klyx.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun KlyxDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    positiveButton: @Composable (() -> Unit)? = null,
    negativeButton: @Composable (() -> Unit)? = null,
    neutralButton: @Composable (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column {
                val isAnyButtonAvailable = listOf(positiveButton, negativeButton, neutralButton).any { it != null }

                Column(
                    modifier = Modifier.padding(
                        top = 16.dp, start = 16.dp, end = 16.dp,
                        bottom = if (isAnyButtonAvailable) 0.dp else 16.dp
                    )
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    if (message != null) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (isAnyButtonAvailable) {
                    Row {
                        if (neutralButton != null) {
                            neutralButton()
                        }

                        if (negativeButton != null) {
                            negativeButton()
                        }

                        if (positiveButton != null) {
                            positiveButton()
                        }
                    }
                }
            }
        }
    }
}
