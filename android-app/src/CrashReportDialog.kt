package com.klyx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun CrashReportDialog(
    report: CrashReport,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpenIssue: () -> Unit,
) {
    var showDetails by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Klyx crashed last time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${report.kind} · ${report.versionName} (${report.versionCode})",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "${report.timestamp} · ${report.device} · ${report.android}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = report.exceptionClass,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = report.message.ifBlank { "(no message)" },
                    style = MaterialTheme.typography.bodyMedium,
                )

                TextButton(onClick = { showDetails = !showDetails }) {
                    Text(if (showDetails) "hide details" else "show details")
                }

                if (showDetails) {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = report.stackTrace,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                            if (report.logTail.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "recent logs\n${report.logTail}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
                TextButton(onClick = onCopy) { Text("Copy") }
                TextButton(onClick = onShare) { Text("Share") }
                TextButton(onClick = onOpenIssue) { Text("GitHub") }
            }
        },
    )
}
