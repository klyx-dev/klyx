package com.klyx.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.lsp.LspActivityStore
import com.klyx.lsp.LspManager
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LspStatusBar(
    store: LspActivityStore,
    lspManager: LspManager,
    modifier: Modifier = Modifier
) {
    val entries by store.entries.collectAsStateWithLifecycle()
    val verbose by store.verbose.collectAsStateWithLifecycle()
    val progress by store.progress.collectAsStateWithLifecycle()
    val servers by store.servers.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val latest = entries.lastOrNull()
    val activeProgress = progress.values.firstOrNull()
    val anyRunning = servers.any { it.running }

    if (servers.isEmpty()) return

    val statusColor by animateColorAsState(
        targetValue = when {
            !anyRunning -> MaterialTheme.colorScheme.onSurfaceVariant
            activeProgress != null -> MaterialTheme.colorScheme.primary
            latest?.severity == LspActivityStore.Severity.Error -> MaterialTheme.colorScheme.error
            latest?.severity == LspActivityStore.Severity.Warning -> Color(0xFFE8A317)
            else -> Color(0xFF3CB371)
        },
        label = "lspStatusColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            servers.forEach { server ->
                val busy = server.running &&
                        progress.values.any { it.server == server.displayName }
                ServerStatusPill(name = server.name, running = server.running, busy = busy)
            }
        }
        Text(
            text = when {
                !anyRunning -> "All language servers stopped"
                activeProgress != null -> buildString {
                    append(shortServerName(serverName(servers, activeProgress.server)))
                    append(" · ")
                    append(activeProgress.title)
                    activeProgress.message?.let { append(": "); append(it) }
                    activeProgress.percentage?.let { append(" (").append(it).append("%)") }
                }

                latest != null -> buildString {
                    append(shortServerName(serverName(servers, latest.server)))
                    append(" · ")
                    append(latest.message)
                }

                else -> "No language-server activity"
            },
            color = if (!anyRunning) MaterialTheme.colorScheme.onSurfaceVariant
            else latest?.severity?.color() ?: MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
        (activeProgress.takeIf { anyRunning })?.let { item ->
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
        LspActivitySheet(
            onDismiss = { expanded = false },
            entries = entries,
            verbose = verbose,
            servers = servers,
            progress = progress.values.toList(),
            onStart = { lspManager.startServer(it) },
            onRestart = { lspManager.restartServer(it) },
            onStop = { lspManager.stopServer(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LspActivitySheet(
    onDismiss: () -> Unit,
    entries: List<LspActivityStore.Entry>,
    verbose: List<LspActivityStore.Entry>,
    servers: List<LspActivityStore.Server>,
    progress: List<LspActivityStore.Progress>,
    onStart: (String) -> Unit,
    onRestart: (String) -> Unit,
    onStop: (String) -> Unit,
) {
    var showVerbose by remember { mutableStateOf(false) }

    val shown = remember(entries, verbose, showVerbose) {
        (if (showVerbose) (entries + verbose).sortedBy { it.timestamp } else entries).asReversed()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(Hidden, setOf(Hidden, Expanded))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            item {
                Text(
                    "Language Servers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            if (servers.isEmpty()) {
                item {
                    Text(
                        "No language servers are currently running.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        progress = progress.filter { it.server == server.displayName },
                        onStart = onStart,
                        onRestart = onRestart,
                        onStop = onStop
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Activity log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Verbose",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = showVerbose, onCheckedChange = { showVerbose = it })
                }
            }

            if (shown.isEmpty()) {
                item {
                    Text(
                        if (showVerbose) "Nothing has been logged yet."
                        else "No messages. Enable Verbose to see raw server trace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(shown) { entry ->
                    ActivityLogRow(
                        entry = entry,
                        serverLabel = serverName(servers, entry.server),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            item { Spacer(Modifier.padding(bottom = 12.dp)) }
        }
    }
}

@Composable
private fun ServerCard(
    server: LspActivityStore.Server,
    progress: List<LspActivityStore.Progress>,
    onStart: (String) -> Unit,
    onRestart: (String) -> Unit,
    onStop: (String) -> Unit,
) {
    val busy = server.running && progress.isNotEmpty()
    val statusLabel = when {
        !server.running -> "Stopped"
        busy -> "Working"
        else -> "Ready"
    }
    val statusColor = when {
        !server.running -> MaterialTheme.colorScheme.onSurfaceVariant
        busy -> MaterialTheme.colorScheme.primary
        else -> Color(0xFF3CB371)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = statusColor
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                server.name + (server.version?.let { " $it" } ?: ""),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            "${server.languageId} · ${server.workspace ?: "No workspace"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        progress.forEach { item ->
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildString {
                            append(item.title)
                            item.message?.let { append(": "); append(it) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    item.percentage?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$it%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
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

        CapabilitiesSection(server.capabilities)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (server.running) {
                FilledTonalButton(
                    onClick = { onRestart(server.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Restart")
                }
                OutlinedButton(
                    onClick = { onStop(server.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Stop")
                }
            } else {
                FilledTonalButton(
                    onClick = { onStart(server.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Start")
                }
            }
        }
    }
}

private fun serverName(servers: List<LspActivityStore.Server>, tag: String): String =
    servers.firstOrNull { it.displayName == tag }?.name ?: tag

private fun shortServerName(name: String): String {
    val parts = name.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> name.take(2).lowercase(Locale.getDefault())
        parts.size == 1 -> parts[0].take(2).lowercase(Locale.getDefault())
        else -> parts.joinToString("") { it.first().lowercase(Locale.getDefault()) }.take(4)
    }
}

private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.ROOT)

@Composable
private fun ActivityLogRow(
    entry: LspActivityStore.Entry,
    serverLabel: String,
    modifier: Modifier = Modifier,
) {
    val color = entry.severity.color()
    Row(modifier = modifier.padding(vertical = 5.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeverityChip(entry.severity, color)
                Spacer(Modifier.width(6.dp))
                ServerTagChip(serverLabel)
                Spacer(Modifier.width(6.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    TIME_FORMAT.format(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                entry.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ServerTagChip(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CapabilitiesSection(capabilities: List<String>) {
    if (capabilities.isEmpty()) {
        Text(
            "No optional capabilities advertised",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
        return
    }

    val collapsedCount = 6
    var expanded by remember { mutableStateOf(false) }
    val canCollapse = capabilities.size > collapsedCount
    val shown = if (expanded || !canCollapse) capabilities else capabilities.take(collapsedCount)

    Column(modifier = Modifier.padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Capabilities",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${capabilities.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            shown.forEach { CapabilityChip(it) }
            if (canCollapse && !expanded) {
                val remaining = capabilities.size - collapsedCount
                CapabilityChip(
                    label = "+$remaining more",
                    highlighted = true,
                    onClick = { expanded = true }
                )
            }
        }
        if (canCollapse && expanded) {
            Text(
                "Show less",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { expanded = false }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CapabilityChip(
    label: String,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val container = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun ServerStatusPill(name: String, running: Boolean, busy: Boolean) {
    val color = when {
        !running -> MaterialTheme.colorScheme.onSurfaceVariant
        busy -> MaterialTheme.colorScheme.primary
        else -> Color(0xFF3CB371)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 2.dp,
                color = color
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SeverityChip(severity: LspActivityStore.Severity, color: Color) {
    val label = when (severity) {
        LspActivityStore.Severity.Error -> "ERROR"
        LspActivityStore.Severity.Warning -> "WARN"
        LspActivityStore.Severity.Info -> "INFO"
        LspActivityStore.Severity.Debug -> "DEBUG"
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

@Composable
private fun LspActivityStore.Severity.color(): Color = when (this) {
    LspActivityStore.Severity.Error -> MaterialTheme.colorScheme.error
    LspActivityStore.Severity.Warning -> Color(0xFFE8A317)
    LspActivityStore.Severity.Info -> MaterialTheme.colorScheme.primary
    LspActivityStore.Severity.Debug -> MaterialTheme.colorScheme.onSurfaceVariant
}
