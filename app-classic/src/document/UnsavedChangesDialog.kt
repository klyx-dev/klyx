package com.klyx.document

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.Workspace
import com.klyx.runtime.workspace.WorkspaceSessionManager
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxDialog
import com.klyx.ui.theme.bodySmall
import com.klyx.ui.theme.border
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.error
import com.klyx.ui.theme.kxSmallShape
import com.klyx.ui.theme.label
import com.klyx.ui.theme.surfaceVariant
import com.klyx.ui.theme.textPrimary
import com.klyx.ui.theme.textSecondary
import com.klyx.ui.theme.typography
import com.klyx.ui.theme.warning
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun UnsavedChangesDialog() {
    val workspace: Workspace = koinInject()
    val persistence: DocumentPersistence = koinInject()
    val sessions: WorkspaceSessionManager = koinInject()
    val backupError by sessions.error.collectAsState()
    val request by workspace.pendingClose.collectAsState()
    val error by persistence.error.collectAsState()
    val scope = rememberCoroutineScope()
    var resolving by remember { mutableStateOf(false) }

    fun resolve(discard: Boolean) {
        if (resolving) return
        resolving = true
        scope.launch {
            try {
                persistence.resolveClose(discard)
            } finally {
                resolving = false
            }
        }
    }

    fun dismissError() {
        persistence.error.value = null
        sessions.error.value = null
    }

    request?.let { pending ->
        val documents = pending.documentIds.mapNotNull(workspace::document)
        val multipleFiles = pending.documentIds.size > 1
        KxDialog(
            visible = true,
            title = "You have unsaved changes",
            subtitle = if (multipleFiles) {
                "Save your changes to these files before closing."
            } else {
                "Save your changes before closing this file."
            },
            onDismissRequest = { if (!resolving) workspace.cancelClose() },
            actions = {
                UnsavedChangesActions(
                    enabled = !resolving,
                    saveLabel = if (resolving) "Working…" else if (multipleFiles) "Save all" else "Save changes",
                    onCancel = workspace::cancelClose,
                    onDiscard = { resolve(discard = true) },
                    onSave = { resolve(discard = false) },
                )
            },
        ) {
            UnsavedFileList(documents)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Discarding removes your unsaved changes. Cancel keeps editing.",
                style = Theme[typography][bodySmall],
                color = Theme[colors][textSecondary],
            )
            (error ?: backupError)?.let { message ->
                Spacer(Modifier.height(12.dp))
                SaveErrorMessage(message)
            }
        }
    }
    if (request == null && (error != null || backupError != null)) {
        KxDialog(
            visible = true,
            title = "File operation failed",
            onDismissRequest = ::dismissError,
            actions = {
                DialogActionButton("Got it", onClick = ::dismissError)
            },
        ) {
            SaveErrorMessage(error ?: backupError.orEmpty())
        }
    }
}

@Composable
private fun UnsavedFileList(documents: List<Document>) {
    val shape = kxSmallShape()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 180.dp)
            .clip(shape)
            .background(Theme[colors][surfaceVariant])
            .border(1.dp, Theme[colors][border], shape)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        documents.forEach { document ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(7.dp)
                        .background(Theme[colors][warning], CircleShape)
                        .semantics { contentDescription = "Unsaved changes" },
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = document.name,
                        style = Theme[typography][label],
                        color = Theme[colors][textPrimary],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val path = when (val location = document.location) {
                        is DocumentLocation.Project -> location.relativePath.toString()
                        is DocumentLocation.Platform -> location.file.path
                    }
                    Text(
                        text = path,
                        style = Theme[typography][bodySmall],
                        color = Theme[colors][textSecondary],
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnsavedChangesActions(
    enabled: Boolean,
    saveLabel: String,
    onCancel: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 360.dp * fontScale) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogActionButton(saveLabel, onSave, Modifier.fillMaxWidth(), enabled)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogActionButton("Cancel", onCancel, Modifier.weight(1f), enabled, KxButtonVariant.Outlined)
                    DialogActionButton("Discard", onDiscard, Modifier.weight(1f), enabled, KxButtonVariant.Ghost, danger = true)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogActionButton("Cancel", onCancel, enabled = enabled, variant = KxButtonVariant.Outlined)
                Spacer(Modifier.weight(1f))
                DialogActionButton("Discard", onDiscard, enabled = enabled, variant = KxButtonVariant.Ghost, danger = true)
                DialogActionButton(saveLabel, onSave, enabled = enabled)
            }
        }
    }
}

@Composable
private fun DialogActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: KxButtonVariant = KxButtonVariant.Filled,
    danger: Boolean = false,
) {
    KxButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = variant,
        danger = danger,
    ) {
        Text(text, style = Theme[typography][label])
    }
}

@Composable
private fun SaveErrorMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 120.dp)
            .clip(kxSmallShape())
            .background(Theme[colors][error].copy(alpha = 0.08f))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        style = Theme[typography][bodySmall],
        color = Theme[colors][error],
    )
}
