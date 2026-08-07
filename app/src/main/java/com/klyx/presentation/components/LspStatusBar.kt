package com.klyx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.lsp.LspActivityStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LspStatusBar(store: LspActivityStore, modifier: Modifier = Modifier) {
    val entries by store.entries.collectAsStateWithLifecycle()
    val progress by store.progress.collectAsStateWithLifecycle()
    val servers by store.servers.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val latest = entries.lastOrNull()

    if (servers.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row {
            Text(
                "LSP · ${servers.size} server${if (servers.size == 1) "" else "s"}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(
            text = progress.values.firstOrNull()?.let { progress ->
                buildString { append(progress.title); progress.message?.let { append(": "); append(it) } }
            } ?: latest?.message ?: "No language-server activity",
            color = latest?.severity?.color() ?: MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth()
        )
        progress.values.firstOrNull()?.let { item ->
            if (item.percentage != null) {
                LinearProgressIndicator(
                    progress = { item.percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            sheetState = rememberBottomSheetState(Hidden, setOf(Hidden, Expanded))
        ) {
            Text("Language Server", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(20.dp))
            if (servers.isEmpty()) {
                Text(
                    "No language servers are currently running.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            } else {
                Text(
                    "Running servers",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                servers.forEach { server ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(server.name + (server.version?.let { " $it" } ?: ""),
                            style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${server.languageId} · ${server.workspace ?: "No workspace"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            server.capabilities.ifEmpty { listOf("No optional capabilities advertised") }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            Text(
                "Activity log",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .padding(horizontal = 20.dp)
            ) {
                items(entries.asReversed()) { entry ->
                    Text(
                        "[${entry.server}] ${entry.message}",
                        color = entry.severity.color(),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LspActivityStore.Severity.color(): Color = when (this) {
    LspActivityStore.Severity.Error -> MaterialTheme.colorScheme.error
    LspActivityStore.Severity.Warning -> Color(0xFFE8A317)
    LspActivityStore.Severity.Info -> MaterialTheme.colorScheme.primary
    LspActivityStore.Severity.Debug -> MaterialTheme.colorScheme.onSurfaceVariant
}
